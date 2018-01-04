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

package hobby.wei.c.remote.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hobby.chenai.nakam.basis.TAG;
import hobby.wei.c.L;

/**
 * @author Wei Chou(weichou2010@gmail.com)
 */
public class KeyUtils {
    private static final TAG.LogTag TAG = new TAG.LogTag(KeyUtils.class.getName());

    private static final char AT = '@';
    private static final char EQ = '=';
    private static final char CT = '?';
    private static final String AND = "&";

//    public static final <P extends Param> String makeMemoryCacheKey(Request request, P param) {
//        String cacheKey = request.getClass().getSimpleName() + AT + request.hashCode() + AND +
//                param.getClass().getSimpleName() + AT + param.hashCode();
//        Class<? extends Param> clazz = param.getClass();
//        /*注意getDeclaredFields是获取本类直接定义的所有变量，包括private的，而getFields是所有本类的和继承的public变量*/
//        Field[] fields = clazz.getFields();
//        boolean first = true;
//        for (Field field : fields) {
//            if (field.isAnnotationPresent(MemCacheKeyIgnore.class)) continue;
//            if (first) {
//                first = false;
//                cacheKey += AT;
//            } else {
//                cacheKey += AND;
//            }
//            cacheKey += field.getName() + EQ + ReflectUtils.getBasicFieldValue(field, param);
//        }
//        L.d(TAG, cacheKey);
//        return cacheKey;
//    }

    public static final String makeDBCacheKey(Api api, Map<String, String> key$value) {
        return makeDBCacheKey(api, key$value, false);
    }

    private static final String makeDBCacheKey(Api api, Map<String, String> key$value, boolean log) {
        String cacheKey = getCategory(api);
        if (api.cacheParams != null && api.cacheParams.length > 0) {
            boolean first = true;
            boolean mapNull = key$value == null;
            String value = null;
            for (String key : api.cacheParams) {
                if (first) {
                    first = false;
                    cacheKey += AT;
                } else {
                    cacheKey += AND;
                }
                if (log) {
                    cacheKey += key + EQ + (mapNull ? CT : key$value.get(key));
                } else {
                    value = key$value.get(key);
                    if (value == null || value.length() == 0) throw new NullPointerException("参数值不能为空");
                    cacheKey += key + EQ + value;
                }
            }
        }
        L.d(TAG, "[makeDBCacheKey] cacheKey: %s.", L.s(cacheKey));
        return cacheKey;
    }

    public static final String getCategory(Api api) {
        return api.name + AT + api.baseUrl + (api.controller == null ? "" : api.controller);
    }

    /**
     * 如果旧的参数包含了新的参数，则删除旧的多余的参数，否则返回null。格式：Name@apiurl@a=x&b=2&c=sdjfksl
     **/
    public static final String updateDBCacheKey(String oldKey, Api newApi) {
        String result = null;
        String category = getCategory(newApi);
        int index = category.length();
        if (oldKey.startsWith(category)) {
            if (oldKey.length() == index) {
                if (newApi.cacheParams == null || newApi.cacheParams.length == 0) {
                    result = category;
                } else {
                    result = null;
                }
            } else if (oldKey.charAt(index) == AT) {
                String keyParams = oldKey.substring(index + 1);
                if (newApi.cacheParams == null || newApi.cacheParams.length == 0) {
                    result = null;
                } else {
                    if (keyParams == null || keyParams.length() == 0) {
                        result = null;
                    } else {
                        String[] param$values = keyParams.split(AND);
                        if (param$values.length < newApi.cacheParams.length) {
                            result = null;
                        } else {
                            List<String> list = new ArrayList<String>(newApi.cacheParams.length);
                            boolean contains = false;
                            for (String key : newApi.cacheParams) {
                                contains = false;
                                for (String kv : param$values) {
                                    if (key.equals(kv.substring(0, kv.indexOf(EQ)))) {
                                        list.add(kv);
                                        contains = true;
                                        break;
                                    }
                                }
                                if (!contains) break;
                            }
                            if (contains) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(category);
                                sb.append(AT);
                                boolean first = true;
                                for (String kv : list) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        sb.append(AND);
                                    }
                                    sb.append(kv);
                                }
                                result = sb.toString();
                            } else {
                                result = null;
                            }
                        }
                    }
                }
            }
        }
        L.d(TAG, "[updateDBCacheKey] key: %s, oldkey: %s.", L.s(result), L.s(oldKey));
        return result;
    }

    public static final boolean isDBCacheKeyChanged(String oldKey, Api newApi) {
        boolean changed = true;
        String category = getCategory(newApi);
        int index = category.length();
        if (oldKey.startsWith(category)) {
            if (oldKey.length() == index) {
                changed = newApi.cacheParams != null && newApi.cacheParams.length > 0;
            } else if (oldKey.charAt(index) == AT) {
                String keyParams = oldKey.substring(index + 1);
                if (newApi.cacheParams == null || newApi.cacheParams.length == 0) {
                    changed = (keyParams != null && keyParams.length() > 0);
                } else {
                    if (keyParams == null || keyParams.length() == 0) {
                        changed = true;
                    } else {
                        String[] param$values = keyParams.split(AND);
                        if (param$values.length != newApi.cacheParams.length) {
                            changed = true;
                        } else {
                            boolean contains = false;
                            for (String key : newApi.cacheParams) {
                                contains = false;
                                for (String kv : param$values) {
                                    if (key.equals(kv.substring(0, kv.indexOf(EQ)))) {
                                        contains = true;
                                        break;
                                    }
                                }
                                if (!contains) break;
                            }
                            changed = !contains;
                        }
                    }
                }
            }
        }
        L.d(TAG, "[isDBCacheKeyChanged] changed: %s, key: %s, oldkey: %s.", changed, L.s(makeDBCacheKey(newApi, null, true)), L.s(oldKey));
        return changed;
    }
}
