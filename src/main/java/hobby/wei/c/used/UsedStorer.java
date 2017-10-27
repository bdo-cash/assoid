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

package hobby.wei.c.used;

import hobby.wei.c.framework.AbsApp;
import hobby.wei.c.persist.Storer;

/**
 * @author 周伟 Wei Chou(weichou2010@gmail.com)
 */
public class UsedStorer {
    public static class absApp extends Storer.Wrapper {
        public static Storer ins() {
            return get(AbsApp.get(), "absapp").multiProcess().ok();
        }

        public static Storer getModule(int moduleId) {
            return get(AbsApp.get(), "absapp_module_" + moduleId).multiProcess().ok();
        }

        public static boolean getFirstLaunch(int moduleId) {
            return getModule(moduleId).loadBoolean("first_launch", true);
        }

        public static void clearFirstLaunch(int moduleId) {
            getModule(moduleId).storeBoolean("first_launch", false);
        }
    }

    public static class device extends Storer.Wrapper {
        public static Storer ins() {
            return get(AbsApp.get(), "device").multiProcess().ok();
        }

        public static String getUniqueId() {
            return ins().loadString("unique_id");
        }

        public static void saveUniqueId(String value) {
            ins().storeString("unique_id", value);
        }
    }

    public static class userHelper extends Storer.Wrapper {
        public static Storer ins() {
            return get(AbsApp.get(), "userhelper").multiProcess().ok();
        }

        public static void saveToken(String value) {
            ins().storeString("token", value);
        }

        public static String getToken() {
            return ins().loadString("token");
        }

        public static void saveAccountJson(String value) {
            ins().storeString("account", value);
        }

        public static String getAccountJson() {
            return ins().loadString("account");
        }

        public static void saveAuthorityFlag(boolean value) {
            ins().storeBoolean("authority_flag", value);
        }

        public static boolean getIsAuthorizeSuccess() {
            return ins().loadBoolean("authority_flag");
        }
    }
}
