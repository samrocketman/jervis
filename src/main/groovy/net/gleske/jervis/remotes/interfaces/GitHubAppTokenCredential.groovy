/*
   Copyright 2014-2022 Sam Gleske - https://github.com/samrocketman/jervis

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

import java.time.Instant

/**
  Provides a high level interface for ephemeral API tokens issued by a GitHub
  App installation.
  */
interface GitHubAppTokenCredential extends TokenCredential {

    /**
      Returns when an issued ephemeral GitHub API token will expire.
      */
    Instant getExpiration()

    /**
      Sets when an ephemeral GitHub API token will expire.
      */
    void setExpiration(Instant expiration)
}

