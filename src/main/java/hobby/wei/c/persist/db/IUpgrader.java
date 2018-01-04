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

import hobby.wei.c.remote.api.Api;

/**
 * @author Wei Chou(weichou2010@gmail.com)
 */
public interface IUpgrader {
    /**
     * 新旧表名称相同。注意tableName和newTable的className可能不一致，取决于newTable类中annotation设置。
     */
    boolean needSaveOldTableData(int oldVersion, String tableName, Class<?> newTable);

    /**
     * 新旧Api名称相同。apiBaseUrl、apiName和newApi的name是一致的。
     */
    boolean needSaveOldApiData(int oldVersion, String apiBaseUrl, String apiName, Api newApi);

    /**
     * 表升级完成。注意{@link Table#ApiCache ApiCacheTable}的升级是直接回调本方法，而没有询问。
     *
     * @param tableName
     * @param clear     是否清空了。未清空则是保留了对应字段的数据。
     */
    void onTableUpgraded(String tableName, boolean clear);
}
