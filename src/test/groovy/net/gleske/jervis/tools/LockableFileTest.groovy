
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
//the LockableFileTest() class automatically sees the LockableFile class because they're in the same package

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LockableFileTest extends GroovyAssert {
    @Rule public TemporaryFolder folder = new TemporaryFolder()
    @Test public void test_LockableFile_concurrent_IO() {
        String tempfile = [(folder.root.path - ~'/$'), 'file'].join('/')
        List<Thread> threads = []
        List complete_order = []

        // Check for file existence
        assert folder.root.exists()
        assert !(new File(tempfile).exists())

        // Delay concurrent locking file by 100ms
        threads += [Thread.start {
            Integer order = threads.size()
            sleep(100)
            new LockableFile(tempfile).withLockedWriter { Writer w ->
                w << 'friend'
            }
            complete_order << 1
        }]

        // Take 200ms to write to file
        threads += [Thread.start {
            new LockableFile(tempfile).withLockedWriter { Writer w ->
                w << 'hello'
                sleep(200)
            }
            complete_order << 2
        }]

        // wait for all concurrent threads to finish
        threads.each { it.join() }

        assert complete_order == [2, 1]
        // Test reading
        assert (new LockableFile(tempfile).getTextWithLock()) == 'friend'
    }
    @Test public void test_LockableFile_lockWaitSleep() {
        String tempfile = [(folder.root.path - ~'/$'), 'file2'].join('/')
        assert folder.root.exists()
        assert !(new File(tempfile).exists())
        LockableFile lockFile = new LockableFile(tempfile)
        assert lockFile.lockWaitSleep == 500
        lockFile.lockWaitSleep = 0
        assert lockFile.lockWaitSleep == 500
        lockFile.lockWaitSleep = -1
        assert lockFile.lockWaitSleep == 500
        lockFile.lockWaitSleep = 1000
        assert lockFile.lockWaitSleep == 1000
    }
}
