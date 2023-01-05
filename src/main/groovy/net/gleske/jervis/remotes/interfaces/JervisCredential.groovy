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
package net.gleske.jervis.remotes.interfaces

/**
   Provides a way to call an external credential store in case there's an
   alternate means of storing and retrieving credentials.  This allows Jervis
   to store credentials securely in a system like Jenkins without having to
   depend on Jenkins for <tt>{@link hudson.util.Secret}</tt>.

 */
interface JervisCredential { }
