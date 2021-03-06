package getl.utils

import getl.proc.Executor
import getl.tfs.TFS
import groovy.transform.Synchronized

/**
 * @author Alexsey Konstantinov
 */
class FileUtilsTest extends GroovyTestCase {
    void testFileExtension() {
        assertEquals('txt', FileUtils.FileExtension('test.txt'))
        assertEquals('txt', FileUtils.FileExtension('/tmp/test.getl/test.txt'))
    }

    void testExcludeFileExtension() {
        assertEquals(FileUtils.ConvertToDefaultOSPath('test'), FileUtils.ExcludeFileExtension('test.txt'))
        assertEquals(FileUtils.ConvertToDefaultOSPath('/tmp/test.getl/test'), FileUtils.ExcludeFileExtension('/tmp/test.getl/test.txt'))
    }

    void testRenameTo() {
        def source = "${TFS.systemPath}/${FileUtils.UniqueFileName()}"
        def destFileName = 'test_rename.txt'
        def dest = "${TFS.systemPath}/$destFileName"

        new File(source).text = 'test'
        FileUtils.RenameTo(source, destFileName)

        assertTrue(FileUtils.DeleteFile(dest))
    }

    void testMoveTo() {
        def fileName = 'test_move.txt'
        def source = "${TFS.systemPath}/$fileName"
        def destPath = "${TFS.systemPath}/test_move"

        new File(source).text = 'test'
        shouldFail { FileUtils.MoveTo(source, destPath, false) }
        FileUtils.MoveTo(source, destPath)
        assertEquals('test', new File("$destPath/$fileName").text)

        new File(source).text = 'test 1'
        FileUtils.MoveTo(source, destPath)
        assertEquals('test 1', new File("$destPath/$fileName").text)

        assertTrue(FileUtils.DeleteFile("$destPath/$fileName"))
        assertTrue(FileUtils.DeleteDir(destPath))
    }

    void testCopyToDir() {
        def fileName = 'test_copy.txt'
        def source = "${TFS.systemPath}/$fileName"
        def destPath = "${TFS.systemPath}/test_copy"

        def f = new File(source)
        f.deleteOnExit()
        f.text = 'test'
        shouldFail { FileUtils.CopyToDir(source, destPath, false) }
        FileUtils.CopyToDir(source, destPath)

        assertTrue(FileUtils.DeleteFile("$destPath/$fileName"))
        assertTrue(FileUtils.DeleteDir(destPath))
    }

    void testCopyToFile() {
        def fileName = 'test_copy.txt'
        def source = "${TFS.systemPath}/$fileName"
        def destPath = "${TFS.systemPath}/test_copy"
        def destFileName = "$destPath/test_copy_new.txt"

        def f = new File(source)
        f.deleteOnExit()
        f.text = 'test'
        shouldFail { FileUtils.CopyToFile(source, destFileName, false) }
        FileUtils.CopyToFile(source, destFileName)

        assertTrue(FileUtils.DeleteFile(destFileName))
        assertTrue(FileUtils.DeleteDir(destPath))
    }

    void testDeleteFolder() {
        def path = "${TFS.systemPath}/test_empty_folder"
        FileUtils.ValidPath(path)
        FileUtils.ValidPath("$path/1/a")
        FileUtils.ValidPath("$path/1/b")
        FileUtils.ValidPath("$path/1/c")
        FileUtils.ValidPath("$path/2")

        new File("$path/1/a/test_empty.txt").text = 'test'
        new File("$path/1/b/test_empty.txt").text = 'test'

        FileUtils.DeleteEmptyFolder(path, true)
        assertTrue(new File("$path/1").exists())
        assertFalse(new File("$path/2").exists())

        assertTrue(FileUtils.DeleteFolder("$path/1", true))
        assertFalse(new File("$path/1").exists())

        FileUtils.DeleteEmptyFolder(path, true)
        assertFalse(new File(path).exists())
    }

    void testLastDirFromPath() {
        assertNull(FileUtils.lastDirFromPath(null as String))
        assertEquals('test.getl', FileUtils.lastDirFromPath("/tmp/test.getl"))
    }

    void testLastDirFromFile() {
        assertNull(FileUtils.lastDirFromFile(null as String))

        def path = "${TFS.systemPath}/test.getl"
        def fileName = "$path/test.txt"

        FileUtils.ValidFilePath(fileName)
        new File(fileName).text = 'test'

        assertEquals('test.getl', FileUtils.lastDirFromFile(fileName))

        FileUtils.DeleteFolder(path, true)
    }

    void testMaskFile() {
        assertNull(FileUtils.MaskFile(null))
        assertNull(FileUtils.MaskFile('/tmp/test.getl/test123_a.txt'))
        assertEquals('*.txt', FileUtils.MaskFile('*.txt'))
        assertEquals('test???_*.txt', FileUtils.MaskFile('/tmp/test.getl/test???_*.txt'))
    }

    void testPathFromFile() {
        assertNull(FileUtils.PathFromFile(null))

        def p = FileUtils.ConvertToDefaultOSPath(TFS.systemPath)
        def f = new File("$p/test_pathfromfile.txt")
        f.deleteOnExit()
        f.text = 'test'

        assertEquals(p, FileUtils.PathFromFile("$p/test_pathfromfile.txt"))
        assertEquals(p, FileUtils.PathFromFile("$p/test_*.txt"))
    }

    void testFileName() {
        assertNull(FileUtils.FileName(null))

        def p = FileUtils.ConvertToDefaultOSPath(TFS.systemPath)
        def f = new File("$p/test_pathfromfile.txt")
        f.deleteOnExit()
        f.text = 'test'

        assertEquals('test_pathfromfile.txt', FileUtils.FileName("$p/test_pathfromfile.txt"))
        assertEquals('test_*.txt', FileUtils.FileName("$p/test_*.txt"))
    }

    void testIsLockFileForRead() {
        assertNull(FileUtils.IsLockFileForRead(null))

        def fileName = "${TFS.systemPath}/test_lock.txt"
        def f = new File(fileName)
        f.deleteOnExit()
        f.text = 'test'

        boolean res
        def lf = FileUtils.LockFile(fileName, 'rw', false)
        assertTrue(FileUtils.IsLockFileForRead(fileName))
        lf.release()
        lf.channel().close()

        assertFalse(FileUtils.IsLockFileForRead(fileName))
    }

    void testConvertText() {
        def original_text = new StringReader('Converted text 12345 test validation')
        def processed_text = new StringWriter()
        def rules = [
                [type: 'REGEXPR', old: 'text.*test', new: 'text-test'],
                [type: 'REPLACE', old: 'e', new: '[E]']
        ]
        FileUtils.ConvertText(original_text, processed_text, rules, null, null)
        assertEquals('Conv[E]rt[E]d t[E]xt-t[E]st validation\n', processed_text.toString())
    }

    void testRun() {
        def sout = new StringBuilder()
        def serr = new StringBuilder()
        if (Config.isWindows()) {
            FileUtils.Run('cmd /c dir', TFS.systemPath, 'utf-8', sout, serr)
        }
        else {
            FileUtils.Run('ls', TFS.systemPath, 'utf-8', sout, serr)
        }
    }
}
