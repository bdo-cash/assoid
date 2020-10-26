/*
 * Copyright (C) 2020-present, Chenai Nakam(chenai.nakam@gmail.com)
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

package hobby.wei.c.persist.db

import java.util.Properties
import slick.jdbc.SQLiteProfile.api._
import org.sqldroid.{DroidDataSource, SQLDroidDriver}

/**
 * @author Chenai Nakam(chenai.nakam@gmail.com)
 * @version 1.0, 26/10/2020
 */
class SlickJdbc {
  lazy val db = Database.forDataSource(ds = new MDS, None)

  // TODO: test, 待改进。
  class MDS extends DroidDataSource {
    protected val sqlite_database_file_path: String = ""
    protected val inMemory: Boolean                 = true

    protected val url = s"jdbc:sqlite:${if (inMemory) ":memory:" else sqlite_database_file_path}"

    override def getConnection = {
      new SQLDroidDriver().connect(url, new Properties)
    }
  }
}
