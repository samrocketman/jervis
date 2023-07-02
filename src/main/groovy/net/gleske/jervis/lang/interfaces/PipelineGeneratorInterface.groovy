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
package net.gleske.jervis.lang.interfaces
import net.gleske.jervis.lang.LifecycleGenerator

/**
  This interface creates a contract of forward
  compatibility for Jervis pipelines.
  */
interface PipelineGeneratorInterface {
    //methods
    LifecycleGenerator getGenerator()
    Set<String> getSupported_collections()
    Map getCollect_settings_defaults()
    Map getCollect_settings_validation()
    Map getCollect_settings_filesets()
    Map getStashmap_preprocessor()
    void setStashmap_preprocessor(Map m)
    void setCollect_settings_defaults(Map m)
    List getBuildableMatrixAxes()
    Map getStashMap()
    Map getStashMap(Map matrix_axis)
    List getSecretPairsEnv()
    List getPublishableItems()
    def getPublishable(String item)
    String getDefaultToolchainsScript()
    Map getDefaultToolchainsEnvironment()
    Map getYaml()
}
