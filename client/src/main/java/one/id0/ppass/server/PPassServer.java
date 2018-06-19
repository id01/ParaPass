package one.id0.ppass.server;

import java.io.FileNotFoundException;
import java.io.IOException;

import one.id0.ppass.backend.Logger;
import one.id0.ppass.backend.PPassBackend;

public interface PPassServer {
	public void init(PPassBackend backend, boolean forceServerKeyChange, Logger logger) throws FileNotFoundException, IOException;
}