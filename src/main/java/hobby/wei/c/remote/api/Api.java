/*
 * Copyright (C) 2017-present, Chenai Nakam(chenai.nakam@gmail.com)
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

import java.util.HashMap;
import java.util.Map;

import hobby.wei.c.data.abs.IParser;
import hobby.wei.c.remote.INetAces.Method;

/**
 * 网络接口数据缓存配置。
 * 与网络接口有关的任务，缓存全部存入一个接口专用的通用数据库表。
 * 是否访问网络跟缓存配置有关。除正常情况`先读缓存后读网络`以外，访问网络【之前】是否访问【缓存】数据库跟具体的任务类型有关。分为两种情况：
 * 1、虽然有缓存，但仍强制访问网络：如登录。应该直接访问网络而不应先访问缓存数据库（登录需要的参数应该在之前的任务中读取）；
 * 2、即使有网络连接，即使缓存为空，也只读缓存而不访问网络：如获取上次登录的用户信息。由于登录之后获取的用户信息是存入接口专用表的，
 * 因此需要访问接口专用表，但是接口专用表是不允许其他非网络接口任务使用的，因此需要配置为接口任务，但是不访问网络。
 *
 * @author Wei Chou(weichou2010@gmail.com)
 * @version 1.0, xx/xx/2013
 */
public abstract class Api {
    public final static long DAY_TIME_MS = 24 * 60 * 60 * 1000;

    protected abstract String key4Uid();

    protected abstract String key4Passport();

    protected abstract String key4PageNum();

    protected abstract String key4PageSize();

    protected abstract int maxPageSize();

    /**
     * 全局唯一的名字，所有Api配置中不可重复。
     * 这里把一个特定接口“controller、参数个数、参数类型”三者的组合看作一个 Category，因此对于同一个 controller 有不同参数的组合的情况，
     * 仅通过 controller 无法标识一个特定类型的请求（Category），因此通过 name 标识。
     */
    public final String name;
    public final String baseUrl;
    public final String controller;
    public final int method;
    /**
     * passport放到header里面。
     */
    public final boolean passport;
    /**
     * 是否分页。
     */
    public final boolean pagination;
    public final Map<String, String> headers;
    public final String[] params;
    public final String[] defValues;
    /**
     * 作为缓存key生成条件的参数，必须包含于 params。要求这些值对于一条记录是不变的且足够标识一条记录。
     * 注意：pageSize 是不应该有的；对于与用户有关的数据，userId 会自动设置为一个参数，这里不需要添加。
     */
    public final String[] cacheParams;
    /**
     * 缓存时间，小于 0 表示没有缓存，0表示无限缓存，不用写入缓存时间。大于 0 则需要写入缓存时间。单位：毫秒。
     */
    public final long cacheTimeMS;
    /**
     * 数据解析器。
     */
    public final IParser parser;

    public Api(String url, String controller, int method, String[] params, long cacheTimeMS, IParser parser) {
        this(null, url, controller, method, false, params, null, params, cacheTimeMS, parser);
    }

    public Api(String url, String controller, int method, String[] params,
               String[] cacheParams, long cacheTimeMS, IParser parser) {
        this(null, url, controller, method, false, params, null, cacheParams, cacheTimeMS, parser);
    }

    public Api(String name, String url, String controller, int method, boolean passport, String[] params, String[] defValues,
               String[] cacheParams, long cacheTimeMS, IParser parser) {
        this(name, url, controller, method, passport, null, params, defValues, cacheParams, cacheTimeMS, parser);
    }

    public Api(String name, String baseUrl, String controller, int method, boolean passport, Map<String, String> headers, String[] params,
               String[] defValues, String[] cacheParams, long cacheTimeMS, IParser parser) {
        if (name == null || name.length() == 0) name = null;
        if (controller == null || controller.length() == 0) controller = null;

        baseUrl = baseUrl.trim().toLowerCase();
        if (!baseUrl.startsWith("http")) throw new IllegalArgumentException("baseUrl必须以`http`开头");
        if (controller != null) {
            controller = controller.trim();
            if (!controller.matches(REGEX)) throw new IllegalArgumentException("controller含有非法字符`" + controller + "`，参见正则`" + REGEX + "`。");
            if (!baseUrl.endsWith("/")) throw new IllegalArgumentException("当controller不为空时，baseUrl必须以`/`结尾。");
        } else {
            if (baseUrl.endsWith("/")) throw new IllegalArgumentException("当controller为空时，baseUrl不能以`/`结尾。");
        }
        if (name != null) {
            name = name.trim();
            if (!name.matches(REGEX)) throw new IllegalArgumentException("name含有非法字符`" + name + "`，参见正则`" + REGEX + "`。");
        } else {
            name = controller;
            if (name == null) throw new IllegalArgumentException("当controller为空时，name不能为空。");
        }

        if (method < Method.DEPRECATED_GET_OR_POST || method > Method.DELETE) {
            throw new IllegalArgumentException("method值不正确，详见" + Method.class.getName());
        }
        if (defValues == null || defValues.length == 0) defValues = null;
        if (defValues != null && defValues.length > params.length) throw new IllegalArgumentException("defValues长度不能大于params。可以为null。");

        if (params == null || params.length == 0) params = null;
        if (cacheParams == null || cacheParams.length == 0) cacheParams = null;
        if (cacheTimeMS >= 0) {
            int len = params == null ? 0 : params.length;
            int clen = cacheParams == null ? 0 : cacheParams.length;
            if (len > 0 && clen == 0) throw new IllegalArgumentException("启用了缓存，cacheParams不能为空。");
            if (passport && clen == 0) throw new IllegalArgumentException("启用了缓存，且数据与用户相关（passport），cacheParams必须含有`" + key4Uid() + "`字段。");
            if (passport) {
                if (clen > len + 1) throw new IllegalArgumentException("启用了缓存，cacheParams长度不能超过params.length + 1。");
            } else {
                if (clen > len) throw new IllegalArgumentException("启用了缓存，cacheParams长度不能超过params。");
            }
        }
        //////////////////// 避免之后其他部分代码的频繁 toLowerCase() 以及由此引发的错误 ///////////////////////////
        boolean pagination = false, pageNo = false, pageSize = false;
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                params[i] = params[i].toLowerCase();
                if (!pageNo && params[i].equals(key4PageNum())) {
                    pagination = pageNo = true;
                    continue;
                }
                if (!pageSize && params[i].equals(key4PageSize())) {
                    pagination = pageSize = true;
                    if (defValues != null && defValues.length > i) {
                        int size = Integer.parseInt(defValues[i]);
                        if (size > maxPageSize()) throw new IllegalArgumentException("pageSize不能超过`" + maxPageSize() + "`。");
                    }
                }
            }
            for (int i = 0; i < params.length; i++) {
                for (int j = i + 1; j < params.length; j++) {
                    if (params[i].equals(params[j]))
                        throw new IllegalArgumentException("params不能有重复：" + params[i] + ", [" + i + "], [" + j + "]。");
                }
            }
        }
        if (pagination && !pageNo) throw new IllegalArgumentException("由于有分页，params必须含有`" + key4PageNum() + "`字段。");
        if (pagination && !pageSize) throw new IllegalArgumentException("由于有分页，params必须含有`" + key4PageSize() + "`字段。");

        boolean userId = pageNo = false;
        if (cacheParams != null && cacheTimeMS >= 0) {
            for (int i = 0; i < cacheParams.length; i++) {
                cacheParams[i] = cacheParams[i].toLowerCase();
                if (passport && !userId && cacheParams[i].equals(key4Uid())) {
                    userId = true;
                    continue;
                }
                if (pagination && !pageNo && cacheParams[i].equals(key4PageNum())) {
                    pageNo = true;
                    continue;
                }
                if (cacheParams[i].equals(key4PageSize())) {
                    throw new IllegalArgumentException("cacheParams不应该含有`" + key4PageSize() + "`字段。");
                }
            }
            for (int i = 0; i < cacheParams.length; i++) {
                for (int j = i + 1; j < cacheParams.length; j++) {
                    if (cacheParams[i].equals(cacheParams[j]))
                        throw new IllegalArgumentException("cacheParams不能有重复：" + cacheParams[i] + ", [" + i + "], [" + j + "]。");
                }
            }
        }
        if (cacheTimeMS >= 0 && passport && !userId)
            throw new IllegalArgumentException("启用了缓存，数据与用户相关（passport），cacheParams必须含有`" + key4Uid() + "`字段。");
        if (cacheTimeMS >= 0 && pagination && !pageNo) throw new IllegalArgumentException("启用了缓存，有分页，cacheParams必须含有`" + key4PageNum() + "`字段。");

        if (cacheTimeMS >= 0) {
            //////////////////// 仅检查 cacheParams 是否包含于 params /////////////////////////////////
            if (cacheParams != null) {
                boolean contain = false, checked = false;
                for (String c : cacheParams) {
                    contain = false;
                    if (params != null) {
                        for (String p : params) {
                            if (c.equals(p)) {
                                contain = true;
                                break;
                            }
                        }
                    }
                    if (!contain) {
                        if (!checked && c.equals(key4Uid())) {
                            checked = true;
                            if (passport) continue;
                        }
                        throw new IllegalArgumentException("cacheParams必须包含于params。passport为true时，`" + key4Uid() + "`字段除外。");
                    }
                }
            }
        }

        this.name = name;
        this.baseUrl = baseUrl;
        this.controller = controller;
        this.method = method;
        this.passport = passport;
        this.pagination = pagination;
        this.headers = headers;
        this.params = params;
        this.defValues = defValues;
        this.cacheParams = cacheParams;
        this.cacheTimeMS = cacheTimeMS;
        this.parser = parser;
    }

    public final Map<String, String> getMappedParams(Map<String, String> paramsMap) {
        Map<String, String> taskParams = new HashMap<String, String>();
        if (defValues != null) {
            for (int i = 0; i < defValues.length; i++) {
                taskParams.put(params[i], defValues[i]);
            }
        }
        if (params != null) {
            String value;
            for (String key : params) {
                value = paramsMap.get(key);
                if (value != null && value.length() > 0) {
                    if (key.equals(key4PageSize()) && Integer.parseInt(value) > maxPageSize()) {
                        throw new IllegalArgumentException("pageSize不能超过`" + maxPageSize() + "`。");
                    }
                    if (key.equals(key4PageNum()) && Integer.parseInt(value) < 1) {
                        throw new IllegalArgumentException("pageNo不能小于1。");
                    }
                    taskParams.put(key, value);
                } else if (!taskParams.containsKey(key)) {
                    taskParams.put(key, value);
                }
            }
        }
        if (cacheParams != null) {
            for (String p : cacheParams) {
                if (!taskParams.containsKey(p)) taskParams.put(p, paramsMap.get(p));
            }
        }
        if (passport) taskParams.put(key4Passport(), paramsMap.get(key4Passport()));
        return taskParams;
    }

    private static final String REGEX = "[A-Za-z_$]+\\d*[A-Za-z_$]*";
}
