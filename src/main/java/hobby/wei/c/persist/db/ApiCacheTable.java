/*
 * Copyright (C) 2014-present, Wei Chou(weichou2010@gmail.com)
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

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

import hobby.wei.c.anno.proguard.Keep$;
import hobby.wei.c.anno.proguard.KeepVp$e;

/**
 * 网络Api接口数据缓存通用数据库表。通常将一次调用Api取得的数据缓存为一条记录。
 *
 * @author Wei Chou(weichou2010@gmail.com)
 * @version 1.0, xx/xx/2013
 */
@Keep$
@KeepVp$e
@DatabaseTable
public class ApiCacheTable {
    public static final String FIELD_KEY = "key";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_DATE = "date";
    public static final String FIELD_RECORD_COUNT = "recordcount";

    /**
     * 唯一标识数据的key。注意，有关用户的信息，只将用户名或id作为生成Key的参数，密码会变，passport也会变。
     **/
    @DatabaseField(id = true, columnName = FIELD_KEY, canBeNull = false)
    public String key;
    @DatabaseField(canBeNull = false)
    public String value;
    /**
     * 该条记录的类型。通常是网络接口名称或实体类名，用于区分不同的记录类别，如用户信息等；也用于接口更改后的数据记录清理。
     **/
    @DatabaseField(columnName = FIELD_CATEGORY, canBeNull = false)
    public String category;
    /**
     * 测试证明：java.sql.Date是不被支持的，即使支持，也只能精确到日，而不能精确到毫秒
     **/
    @DatabaseField(columnName = FIELD_DATE, dataType = DataType.DATE_STRING)
    public Date date;
    @DatabaseField(columnName = FIELD_RECORD_COUNT)
    public int recordCount;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }
}
