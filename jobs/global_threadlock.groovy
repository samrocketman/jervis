/*
   Copyright 2014-2023 Sam Gleske - https://github.com/samrocketman/jervis

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   */
/*
   A global thread lock for parallelized groovy scripts.
 */

//http://chrisbroadfoot.id.au/2008/08/06/groovy-threads/
//http://docs.groovy-lang.org/latest/html/gapi/groovy/transform/Synchronized.html
//https://github.com/jenkinsci/jenkins-scripts/blob/master/scriptler/findOfflineSlaves.groovy

import java.util.concurrent.locks.ReentrantLock

ReentrantLock.metaClass.withLock = {
  lock()
  try {
    it()
  }
  finally {
      unlock()
  }
}

//groovy binding
global_threadlock = new ReentrantLock()
