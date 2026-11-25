package samples;

import java.util.HashMap;

public class KeyDemo {
    public class FileKey {
        private String filePath;

        public FileKey(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;

            return obj instanceof FileKey && filePath.equals(((FileKey) obj).filePath);
        }

        @Override
        public int hashCode() {
            return filePath.hashCode();
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }

    public static void main(String[] args) {
        HashMap<FileKey, String> map = new HashMap<>();
        FileKey key1 = new KeyDemo().new FileKey("path/to/file1");
        FileKey key2 = new KeyDemo().new FileKey("path/to/file1");

        map.put(key1, "Parsed");

        System.out.println("map.get(key2): " + map.get(key2));

        key1.setFilePath("path/to/file2");
        System.out.println("map.get(key1): " + map.get(key1));
        System.out.println("map.get(key2): " + map.get(key2));
        System.out.println("map.size(): " + map.size());
    }
}