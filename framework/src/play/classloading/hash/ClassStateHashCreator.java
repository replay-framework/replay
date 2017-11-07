package play.classloading.hash;

import play.vfs.VirtualFile;

import java.util.List;

public class ClassStateHashCreator {
    public synchronized int computePathHash(List<VirtualFile> paths) {
        return 0; // this disables the original Play's calculation, which is quite slow
    }
}
