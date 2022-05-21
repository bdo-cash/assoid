///*
// * Copyright (C) 2014-present, Wei Chou(weichou2010@gmail.com)
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package hobby.wei.c.persist.db;
//
//import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
//import com.j256.ormlite.dao.Dao;
//
//import java.lang.reflect.ParameterizedType;
//import java.sql.SQLException;
//
///**
// * @author Wei Chou(weichou2010@gmail.com)
// * @version 1.0, xx/xx/2013
// */
//public abstract class Table<T, ID> {
//    public static final String TAG = "Table<T, ID>";
//    public static final Table<ApiCacheTable, String> ApiCache = new Table<ApiCacheTable, String>() {
//    };
//
//    public final Class<T> clazz;
//
//    @SuppressWarnings("unchecked")
//    public Table() {
//        clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
//        if (ApiCache != null && clazz.getSimpleName().equalsIgnoreCase(ApiCache.clazz.getSimpleName()))
//            throw new IllegalArgumentException("自定义数据库实体类名称`不`应为`" + ApiCache.clazz.getSimpleName() + "`。");
//    }
//
//    public final Dao<T, ID> getDao(OrmLiteSqliteOpenHelper helper) throws SQLException {
//        return helper.getDao(clazz);
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        return o instanceof Table && ((Table<?, ?>) o).clazz == clazz;
//    }
//}
