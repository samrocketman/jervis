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
  Provides a high level interface for providing GitHub App credentials.  This
  enables implementing a lightweight credential resolver for Jenkins where the
  credential is not stored in the class but instead only queried from Jenkins
  credentials backend as needed.  It could also be stored in an alternate
  credential backend such as Vault.
  */
interface GitHubAppRsaCredential extends JervisCredential {


    /**
      Returns an ID unique to this credential to differentiate it from other
      credentials.
      */
    String getId()

    /**
      Returns the GitHub App ID for the app installed into GitHub orgs and
      users.
      */
    String getAppID()

    /**
      Returns the API URL where this class will attempt to submit a GitHub app.
      This would return <tt>https://api.github.com</tt> unless otherwise set to
      a hosted GitHub Enterprise instance.
      */
    String getApiUri()

    /**
      Returns the owner which installed the GitHub App.  This can return an
      empty <tt>String</tt> if no owner is set.
      */
    String getOwner()

    /**
      Returns the RSA private key for a GitHub App.  This will be used to sign
      JWT tokens for authentication with the GitHub API.
      */
    String getPrivateKey()
}
