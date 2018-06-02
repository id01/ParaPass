package one.id0.ppass;

public class Logger {
	public static void log(String toLog) {
		System.out.println("[LOG] " + toLog);
	}
	
	public static void logErr(String toLog) {
		System.out.println("[ERROR] " + toLog);
	}
}