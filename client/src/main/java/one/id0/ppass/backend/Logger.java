package one.id0.ppass.backend;

// Class for logging of events
public class Logger {
	// Log Handlers
	public Handler onLog;
	public Handler onError;
	
	// Log Handler Interface
	public static interface Handler {
		public abstract Runnable getRunnable(String toLog);
	}
	
	// Default log handler
	public class LogHandler implements Handler {
		public Runnable getRunnable(String toLog) {
			return new Runnable() {
				public void run() {
					System.out.println("[LOG] " + toLog);
				}
			};
		}
	}
	
	// Default error handler
	public class ErrorHandler implements Handler {
		public Runnable getRunnable(String toLog) {
			return new Runnable() {
				public void run() {
					System.out.println("[ERROR] " + toLog);
				}
			};
		}
	}
	
	// Constructors
	public Logger() {
		onLog = new LogHandler();
		onError = new ErrorHandler();
	}
	public Logger(Handler onLog, Handler onError) {
		this.onLog = onLog;
		this.onError = onError;
	}
	
	// Log functions
	public void log(String toLog) {
		onLog.getRunnable(toLog).run();
	}
	public void logErr(String toLog) {
		onError.getRunnable(toLog).run();
	}
}