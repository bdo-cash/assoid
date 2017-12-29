/*
 * Copyright (C) 2014-present, Wei Chou (weichou2010@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hobby.wei.c.persist.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hobby.wei.c.L;
import hobby.wei.c.remote.api.Api;
import hobby.wei.c.remote.api.KeyUtils;

/**
 * @author Wei Chou(weichou2010@gmail.com)
 */
public abstract class ApiCacheOrmLiteHelper extends AbsOrmLiteHelper {
    public static final Table<ApiCacheTable, String> sApiCacheTable = Table.ApiCache;

    public ApiCacheOrmLiteHelper(Context context, String dbName, int dbVersion) {
        super(context, dbName, dbVersion);
    }

    protected abstract Set<Api> getApis();

    @Override
    protected void onCreateOthers(SQLiteDatabase database, ConnectionSource connSource) {
        // 创建接口数据缓存表
        try {
            TableUtils.createTableIfNotExists(connSource, sApiCacheTable.clazz);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Set<String> onUpgradeOthers(SQLiteDatabase database, ConnectionSource connSource, int oldVersion, int newVersion, Map<String, String> oldTableSqls) {
        String apiCacheTableSql = getApiCacheTableSql(connSource);
        String apiCacheTableName = parseTableNameInSql(apiCacheTableSql, false, null);

        // 接口数据缓存表升级
        String oldApiCacheTableSql = oldTableSqls.remove(apiCacheTableName);
        if (oldApiCacheTableSql != null) {
            if (!apiCacheTableSql.equals(oldApiCacheTableSql)) {
                upgradeTable(database, connSource, sApiCacheTable.clazz, apiCacheTableName, oldApiCacheTableSql, apiCacheTableSql);
                getUpgrader().onTableUpgraded(apiCacheTableName, false);
            }
        }

        // 接口数据缓存表导Api数据
        L.d(TAG, "接口数据缓存表导Api数据：");
        Dao<ApiCacheTable, String> dao = null;
        try {
            dao = getDao(sApiCacheTable.clazz);
            Set<String> oldApiCategorys = getOldApiCategorys(dao);
            if (oldApiCategorys != null && oldApiCategorys.size() > 0) {
                Set<Api> newApis = getApis();
                if (newApis != null && newApis.size() > 0) {
                    String category;
                    for (Api api : newApis) {
                        category = KeyUtils.getCategory(api);
                        L.d(TAG, "接口数据缓存表导Api数据：%s.", category);
                        if (oldApiCategorys.remove(category)) {
                            if (api.cacheTimeMS >= 0) {
                                upgradeApiCache(oldVersion, dao, api, category);
                            } else {
                                L.d(TAG, "cacheTimeMS < 0 删除不用缓存的数据。category：%s.", category);
                                DeleteBuilder<ApiCacheTable, String> deleteBuilder = dao.deleteBuilder();
                                deleteBuilder.where().eq(ApiCacheTable.FIELD_CATEGORY, category);
                                deleteBuilder.delete();
                            }
                        }
                    }
                    L.d(TAG, "接口数据缓存表导Api数据完毕，删除多余数据。names: %s.", oldApiCategorys);
                    DeleteBuilder<ApiCacheTable, String> deleteBuilder = dao.deleteBuilder();
                    deleteBuilder.where().in(ApiCacheTable.FIELD_CATEGORY, oldApiCategorys);
                    deleteBuilder.delete();
                } else {
                    L.w(TAG, "没有接口，清空表------------");
                    dao.deleteBuilder().delete();    // 清空表
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            L.e(TAG, e, "接口数据缓存表导Api数据异常：");
        } finally {
            if (dao != null) {
                try {
                    dao.closeLastIterator();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dao.clearObjectCache();
            }
        }
        return Collections.singleton(apiCacheTableName);
    }

    private Set<String> getOldApiCategorys(Dao<ApiCacheTable, String> dao) {
        Set<String> categorys = null;
        try {
            List<ApiCacheTable> list = dao.queryBuilder().distinct().selectColumns(ApiCacheTable.FIELD_CATEGORY).query();
            if (list != null) {
                categorys = new HashSet<>(list.size());
                for (ApiCacheTable table : list) {
                    categorys.add(table.category);
                    L.i(TAG, "OldApiCategory: %s.", table.category);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categorys;
    }

    private CloseableIterator<ApiCacheTable> getApiCacheList(Dao<ApiCacheTable, String> dao, String categoryValue) {
        CloseableIterator<ApiCacheTable> iterator = null;
        try {
            QueryBuilder<ApiCacheTable, String> queryBuilder = dao.queryBuilder();
            queryBuilder.selectColumns(ApiCacheTable.FIELD_KEY);
            iterator = queryBuilder.where().eq(ApiCacheTable.FIELD_CATEGORY, categoryValue).iterator();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return iterator;
    }

    private void upgradeApiCache(int oldVersion, Dao<ApiCacheTable, String> dao, Api newApi, String category) {
        CloseableIterator<ApiCacheTable> iterator = getApiCacheList(dao, category);
        if (iterator != null) {
            String newKey;
            boolean test = true, saveOldData = true;
            ApiCacheTable table;
            while (iterator.hasNext()) {
                table = iterator.next();
                try {
                    if (test) {
                        if (!KeyUtils.isDBCacheKeyChanged(table.key, newApi)) {
                            L.d(TAG, "[upgradeApiCache] 没有改变：%s.", category);
                            break;
                        }
                        saveOldData = getUpgrader().needSaveOldApiData(oldVersion, newApi.baseUrl, newApi.name, newApi);
                        L.w(TAG, "[upgradeApiCache] 是否保存旧数据：%s, %s.", saveOldData, category);
                        if (!saveOldData) {
                            break;
                        }
                        test = false;
                    }
                    newKey = KeyUtils.updateDBCacheKey(table.key, newApi);
                    if (newKey == null || newKey.length() == 0) {
                        // delete
                        dao.deleteById(table.key);
                        L.d(TAG, "[upgradeApiCache] 删除：%s.", table.key);
                    } else if (newKey.equals(table.key)) {
                        // 保持不变
                        L.d(TAG, "[upgradeApiCache] 保持不变：%s.", table.key);
                    } else {
                        // 注意每次调用dao.updateBuilder()都会new一个新的。
                        UpdateBuilder<ApiCacheTable, String> updateBuilder = dao.updateBuilder();
                        updateBuilder.where().eq(ApiCacheTable.FIELD_KEY, table.key);
                        updateBuilder.updateColumnValue(ApiCacheTable.FIELD_KEY, newKey);
                        L.d(TAG, "[upgradeApiCache] 更新语句：%s.", updateBuilder.prepareStatementString());
                        updateBuilder.update();
                        L.d(TAG, "[upgradeApiCache] 更新 oldkey: %s, newkey: %s.", table.key, newKey);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    L.e(TAG, "[upgradeApiCache] category: %s, SQLException: %s.", category, e.getLocalizedMessage());
                    L.e(TAG, "发生异常，则清空数据。category: %s.", category);
                    saveOldData = false;
                }
            }
            try {
                iterator.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!saveOldData) {
                try {
                    DeleteBuilder<ApiCacheTable, String> deleteBuilder = dao.deleteBuilder();
                    deleteBuilder.where().eq(ApiCacheTable.FIELD_CATEGORY, category);
                    deleteBuilder.delete();
                    L.w(TAG, "[upgradeApiCache] 删除旧数据。%s.", category);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getApiCacheTableSql(ConnectionSource connSource) {
        String tableSql = null;
        try {
            List<String> statements = TableUtils.getCreateTableStatements(connSource, sApiCacheTable.clazz);
            if (statements != null) {
                for (String sql : statements) {
                    tableSql = sql.trim();    // trim()很重要
                    if (tableSql == null) continue;
                    L.i(TAG, "ApiCacheTableName: %s, ApiCacheTableSql: %s.", sApiCacheTable.clazz.getSimpleName(), tableSql);
                    break;
                }
            }
        } catch (SQLException e) {
            L.e(TAG, e, "ApiCacheTableName: %s.", sApiCacheTable.clazz.getSimpleName());
        }
        return tableSql;
    }
}
