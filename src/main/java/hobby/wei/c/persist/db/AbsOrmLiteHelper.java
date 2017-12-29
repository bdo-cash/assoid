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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hobby.chenai.nakam.basis.TAG;
import hobby.wei.c.L;

/**
 * @author Wei Chou(weichou2010@gmail.com)
 */
public abstract class AbsOrmLiteHelper extends OrmLiteSqliteOpenHelper {
    public static TAG.LogTag TAG = new TAG.LogTag(AbsOrmLiteHelper.class.getName());

    private boolean mCreatingOrUpgrading = false;

    /**
     * 如果用OpenHelperManager.getHelper(this, DBHelper.class)，则应该在子类中增加这样的构造方法：
     * <pre>
     *   public AbsOrmLiteHelper(Context context) {
     *     super(context, dbName, dbVersion);
     *   }
     * </pre>
     */
    public AbsOrmLiteHelper(Context context, String dbName, int dbVersion) {
        super(context, dbName, null, dbVersion);
    }

    protected abstract IUpgrader getUpgrader();

    protected abstract Set<Table<?, ?>> getTables();

    private Set<Class<?>> getTableClasses() {
        Set<Class<?>> tableClazs = new HashSet<>();
        Set<Table<?, ?>> tables = getTables();
        if (tables != null) {
            for (Table<?, ?> table : tables) {
                tableClazs.add(table.clazz);
            }
        }
        return tableClazs;
    }

    protected void onCreateOthers(SQLiteDatabase database, ConnectionSource connSource) {
    }

    protected Set<String> onUpgradeOthers(SQLiteDatabase database, ConnectionSource connSource,
                                          int oldVersion, int newVersion, Map<String, String> oldTableSqls) {
        return Collections.emptySet();
    }

    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        checkIsInitializing();
        return super.getReadableDatabase();
    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        checkIsInitializing();
        return super.getWritableDatabase();
    }

    @Override
    public ConnectionSource getConnectionSource() {
        checkIsInitializing();
        return super.getConnectionSource();
    }

    private void checkIsInitializing() {
        // 此时外部不可以操作数据库，因为还没有创建或升级完成，创建表的时候更不可以调用get方法获取连接，而应该使用方法携带的参数。
        if (mCreatingOrUpgrading) throw new IllegalStateException("不能在`onCreate()`或`onUpgrade()`未结束的时候调用本方法。");
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connSource) {
        L.d(TAG, "onCreate--------");
        mCreatingOrUpgrading = true;

        // 先创建其它的，比如 Api 缓存表。
        onCreateOthers(database, connSource);

        /* 过滤接口数据缓存表（虽然无法创建与接口数据缓存表相同的类名，但是可以通过注解指定相同的表名称。
         * 下面的创建语句已经实现了过滤，这里省略）*/

        for (Class<?> clazz : getTableClasses()) {
            try {
                TableUtils.createTableIfNotExists(connSource, clazz);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        mCreatingOrUpgrading = false;
        L.d(TAG, "onCreate------END");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connSource, int oldVersion, int newVersion) {
        mCreatingOrUpgrading = true;

        L.i(TAG, "Tables In DB: ");
        // 读取旧表信息
        Map<String, String> oldTableSqls = getOldTableSqls(database, false, null);

        // 没有旧表，直接创建
        if (oldTableSqls == null || oldTableSqls.size() == 0) {
            onCreate(database, connSource);
        } else {
            // 先升级其它的，比如 Api 缓存表。
            Set<String> otherTableNames = onUpgradeOthers(database, connSource, oldVersion, newVersion, oldTableSqls);
            if (otherTableNames != null) for (String other : otherTableNames) {
                oldTableSqls.remove(other);
            }
            L.i(TAG, "New Tables: ");

            Set<Class<?>> tables = getTableClasses();
            if (tables == null || tables.size() == 0) {
                // 删除所有旧表
                deleteTables(database, oldTableSqls.keySet().toArray(new String[oldTableSqls.size()]));
                L.w(TAG, "更新版本：新数据库表列表为空，将清空所有旧表。oldVersion: %s, newVersion: %s.", oldVersion, newVersion);
            } else {
                Map<String, Class<?>> newTables = new HashMap<>(tables.size());
                Map<String, String> newTableSqls = new HashMap<>(tables.size());
                makeNewTableSqls(connSource, tables, newTables, newTableSqls, true, otherTableNames);

                L.i(TAG, "Upgrade Tables: ");

                Set<String> nameKeys = newTableSqls.keySet();
                String createSql, oldSql;
                for (String name : nameKeys) {
                    createSql = newTableSqls.get(name);
                    oldSql = oldTableSqls.remove(name);
                    if (oldSql == null) {
                        // 简化处理，只有表名相同的才进行升级，最终没有匹配的旧表将被删除，新表会全部创建
                        // 旧表不存在。这里先不创建新表，最后一次性创建
                        continue;
                    }
                    if (createSql.equals(oldSql)) {    //表结构没有变更，不用导数据
                        L.i(TAG, "Upgrade Tables。表结构没有变更： %s.", name);
                        // newTableSqls不删除，需要在最后再执行一遍创建，因为TableUtils.getCreateTableStatements()返回是是一个list。
                        continue;    // 表结构相同，不用执行升级操作
                    } else {
                        if (getUpgrader().needSaveOldTableData(oldVersion, name, newTables.get(name))) {
                            // 导数据
                            upgradeTable(database, connSource, newTables.get(name), name, oldSql, createSql);
                            getUpgrader().onTableUpgraded(name, false);
                        } else {
                            // 删除旧表
                            deleteTable(database, name);
                            getUpgrader().onTableUpgraded(name, true);
                        }
                    }
                }
                // 删除多余的旧表
                deleteTables(database, oldTableSqls.keySet().toArray(new String[oldTableSqls.size()]));
                // 创建所有没有创建的表，避免遗漏
                onCreate(database, connSource);
            }
        }
        mCreatingOrUpgrading = false;
    }

    private Map<String, String> getOldTableSqls(SQLiteDatabase database, boolean except, Set<String> exceptTableNames) {
        Map<String, String> sqls = null;
        String createSql;
        String tableName;
        Cursor cursor = database.query("sqlite_master", new String[]{"sql"}, "type='table'", null, null, null, "tbl_name");
        if (cursor != null) {
            sqls = new HashMap<>(cursor.getCount());
            while (cursor.moveToNext()) {
                createSql = cursor.getString(0).trim();
                tableName = parseTableNameInSql(createSql, except, exceptTableNames);
                if (tableName == null) continue;    //注意有些是系统表
                sqls.put(tableName, createSql);

                L.i(TAG, "TableName: %s, SQL: %s.", tableName, createSql);
            }
            cursor.close();
        }
        return sqls;
    }

    private void makeNewTableSqls(ConnectionSource connSource, Set<Class<?>> tables, Map<String, Class<?>> newTables,
                                  Map<String, String> newTableSqls, boolean except, Set<String> exceptTableNames) {
        List<String> statements;
        String tableName;
        for (Class<?> clazz : tables) {
            try {
                // 为什么是个List，暂时忽略，测试只看到一个元素，即创建表的语句。
                statements = TableUtils.getCreateTableStatements(connSource, clazz);
                if (statements != null) {
                    for (String sql : statements) {
                        sql = sql.trim();    //trim()很重要
                        tableName = parseTableNameInSql(sql, except, exceptTableNames);
                        if (tableName == null) continue;
                        newTableSqls.put(tableName, sql);
                        newTables.put(tableName, clazz);

                        L.i(TAG, "Table Class: %s, TableName: %s, SQL: %s.", clazz.getSimpleName(), tableName, sql);
                    }
                }
            } catch (SQLException e) {
                L.e(TAG, "Table Class: %s, SQLException: %s.", clazz.getSimpleName(), e.getLocalizedMessage());
            }
        }
    }

    /**
     * 导数据。查询数据到内存，删除旧表，创建新表，写入数据。
     */
    protected static void upgradeTable(SQLiteDatabase database, ConnectionSource connSource, Class<?> tableClazz, String name, String oldSql, String createSql) {
        L.i(TAG, "[upgradeTable start]Table: %s.", name);

        String[] fields = getSameFields(oldSql, createSql);
        if (fields == null || fields.length == 0) return;

        StringBuilder fstr = new StringBuilder();
        boolean b = false;
        for (String s : fields) {
            if (b) {
                fstr.append(", ");
            } else {
                b = true;
            }
            fstr.append(DOT);
            fstr.append(s);
            fstr.append(DOT);
        }

        String name_backup = DOT + name + "_backup" + DOT;
        name = DOT + name + DOT;
        // 以下事务是官方写法
        database.beginTransaction();
        try {
            // 导入有效数据
            database.execSQL("CREATE TEMPORARY TABLE " + name_backup + "(" + fstr + ")");
            database.execSQL("INSERT INTO " + name_backup + " SELECT " + fstr + " FROM " + name);
            database.execSQL("DROP TABLE " + name);
            TableUtils.createTable(connSource, tableClazz);
            database.execSQL("INSERT INTO " + name + "(" + fstr + ")" + " SELECT " + fstr + " FROM " + name_backup);
            database.execSQL("DROP TABLE " + name_backup);

            database.setTransactionSuccessful();
            L.i(TAG, "[upgradeTable Successful]Table: %s.", name);
        } catch (Exception e) {
            database.execSQL("DROP TABLE " + name);
            L.i(TAG, "[upgradeTable Exception][SQL]DROP TABLE %s.", name);
        } finally {
            database.endTransaction();
        }
    }

    protected static void deleteTables(SQLiteDatabase database, String[] tableNames) {
        for (String table : tableNames) {
            deleteTable(database, table);
        }
    }

    protected static void deleteTable(SQLiteDatabase database, String tableName) {
        tableName = DOT + tableName + DOT;
        String sql = "DROP TABLE IF EXISTS " + tableName;
        L.w(TAG, "[deleteTable][SQL]%s.", sql);
        database.execSQL(sql);
    }

    protected static String parseTableNameInSql(String createSql, boolean except, Set<String> exceptTableNames) {
        if (!createSql.substring(0, 12).equalsIgnoreCase("CREATE TABLE")) {
            L.i(TAG, "[getTableName] not create SQL: %s.", createSql);
            return null;
        }
        int $ = createSql.indexOf('(');    // 注意有可能是CREATE TABLE IF NOT EXISTS
        String sys = createSql.substring(0, $);
        if (sys.contains("android_") || sys.contains("sqlite_") || sys.contains("metadata")) {
            L.i(TAG, "[getTableName] systable SQL: %s.", createSql);
            return null;
        }

        int s = createSql.indexOf(DOT) + 1;
        int e = createSql.indexOf(DOT, s);
        if (e <= 0 || e > $) {    // 系统表中没有这些字符
            L.i(TAG, "[getTableName]Index s: %s, e: %s, SQL: %s.", s, e, createSql);
            return null;
        }
        String name = createSql.substring(s, e);
        if (except && exceptTableNames.contains(name)) {
            L.i(TAG, "[getTableName] except exceptNames: %s.", exceptTableNames);
            return null;
        }
        return name;
    }

    private static String[] getSameFields(String oldSql, String createSql) {
        List<String> list = new LinkedList<>();
        oldSql = oldSql.substring(oldSql.indexOf('(') + 1, oldSql.indexOf(')'));
        createSql = createSql.substring(createSql.indexOf('(') + 1, createSql.indexOf(')'));

        String[] oldFields = oldSql.split(",");
        String[] newFields = createSql.split(",");

        for (int i = 0; i < 2; i++) {
            String[] arr = i == 0 ? oldFields : newFields;
            for (int j = 0; j < arr.length; j++) {
                arr[j] = arr[j].trim();
            }
        }
        if (oldFields.length > newFields.length) {
            String[] arr = newFields;
            newFields = oldFields;
            oldFields = arr;
        }
        List<String> list2 = new LinkedList<>();
        Collections.addAll(list2, oldFields);
        String fieldName;
        for (String s : newFields) {
            for (int i = 0, len = list2.size(); i < len; i++) {
                if (s.equalsIgnoreCase(list2.get(i))) {
                    fieldName = parseFieldName(s);
                    if (fieldName != null && fieldName.length() > 0) list.add(fieldName);
                    list2.remove(i);
                    break;
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    private static String parseFieldName(String field) {
        String result;
        int index = field.indexOf(DOT);
        if (index >= 0) {
            int $index = field.indexOf('(');
            if ($index >= 0 && $index < index) {    // PRIMARY KEY (`key_param`)
                result = null;
                L.i(TAG, "[parseFieldName] 带有括号(``): %s.", field);
            } else {
                index++;
                result = field.substring(index, field.indexOf(DOT, index));
            }
        } else {
            char firstChar = field.charAt(0);
            if (firstChar >= 'A' && firstChar <= 'Z') {    // 数据库关键字
                result = null;
                L.i(TAG, "[parseFieldName] 数据库关键字开头：%s.", firstChar);
            } else {
                result = field.split(" ")[0].trim();
            }
        }
        L.i(TAG, "[parseFieldName] result: %s.", result);
        return result;
    }

    public static char DOT = '`';
}
