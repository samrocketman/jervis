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

package net.gleske.jervis.tools

import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
// java.io.RandomAccessFile
// java.io.File

/**
  Provides extensions to <tt>{@link java.io.File}</tt> for exclusive file
  locking to guarantee serialized access to the <tt>File</tt> via
  <tt>{@link java.nio.channels.FileLock}</tt>.
  */
class LockableFile extends File {

    /**
      The interval to wait between trying to obtain the file lock.  This value
      is milliseconds.  Defaults to <tt>500</tt> milliseconds.
     */
    Integer lockWaitSleep = 500

    void setLockWaitSleep(Integer value) {
        if(!value || value < 0) {
            lockWaitSleep = 500
            return
        }
        lockWaitSleep = value
    }

    /**
      Same as a <tt>{@link java.io.File}</tt> with extra functionality for
      creating exclusive locks on the <tt>File</tt> via
      <tt>{@link java.nio.channels.FileLock}</tt>.

      @param path A path to a file.
      */
    LockableFile(String path) {
        super(path)
    }

    /**
      Creates an exclusive lock with the current <tt>LockableFile</tt>.

<pre><code>
import net.gleske.jervis.tools.LockableFile

new LockableFile('/path/to/file').withLock { ->
    // Do anything while having an exclusive read-write lock on the LockableFile
    println('Nothing else has the lock!')
}
</code></pre>

      @param body A Closure to execute while a lock is obtained on the file.
      */
    void withLock(Closure body) {
        FileLock lock
        final RandomAccessFile fileaccess = new RandomAccessFile(getAbsoluteFile(), 'rw')
        try {
            // A do-while loop
            while({ ->
                try {
                    return !(lock=fileaccess.getChannel().tryLock())
                } catch(OverlappingFileLockException ignored) {
                    // return true to continue while loop
                    true
                }
            }()) {
                sleep(new Random().nextInt(this.lockWaitSleep))
            }
            body()
        } finally {
            lock?.close()
            fileaccess.close()
        }
    }

    /**
      Obtains an exclusive <tt>{@link java.nio.channels.FileLock}</tt> before
      reading the contents of a <tt>{@link java.io.File}</tt>.

      <h2>Sample usage</h2>

<pre><code>
import net.gleske.jervis.tools.LockableFile

// Gets the contents of the file while guaranteeing no other LockableFile is
// reading or writing.
String contents = new LockableFile('/path/to/file').getTextWithLock()
</code></pre>

      @return The contents of the <tt>{@link java.io.File}</tt>.
      */
    String getTextWithLock() {
        String contents
        withLock {
            contents = getText()
        }
        contents ?: ''
    }

    /**
      Obtains an exclusive <tt>{@link java.nio.channels.FileLock}</tt> before
      opening a file for writing.

      <h2>Sample usage</h2>

<pre><code>
import net.gleske.jervis.tools.LockableFile

new LockableFile('/path/to/file').withLockedWriter { Writer w ->
    w << 'writing to the file'
}
</code></pre>

      @param body A Closure which must accept a <tt>{@link java.io.Writer}</tt>
                  parameter.
      */
    void withLockedWriter(Closure body) {
        withLock {
            withWriter(body)
        }
    }
}
