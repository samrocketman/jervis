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
   Hacks the Job DSL class loader runtime to inject classes and make them
   available from the uber classloader.
 */
import jenkins.model.Jenkins

hack_class_loader = null
hack_class_loader = { ClassLoader cl, String clazz ->
    String clazz_resource = "${clazz.tokenize('.').join('/')}.class".toString()
    byte[] clazz_bytes = Jenkins.instance.pluginManager.uberClassLoader.getResourceAsStream(clazz_resource)?.getBytes()
    if(!clazz_bytes) {
        throw new Exception("ERROR: hack_class_loader was not able to find class ${clazz} from the pluginManager Uber ClassLoader")
    }
    cl.defineClass(clazz, clazz_bytes)
}
