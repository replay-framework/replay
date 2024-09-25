package play.utils;

/** OS detections */
public class OS {

  public static boolean isWindows() {
    return getOsName().contains("win");
  }

  public static boolean isMac() {
    return getOsName().toLowerCase().contains("mac");
  }

  public static boolean isUnix() {
    var osName = getOsName();
    return osName.contains("nix") || osName.contains("nux") || osName.indexOf("aix") > 0;
  }

  public static boolean isSolaris() {
    return getOsName().contains("sunos");
  }

  private static String getOsName() {
    return System.getProperty("os.name").toLowerCase();
  }
}
