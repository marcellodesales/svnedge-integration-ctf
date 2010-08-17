class LogFile:
    "A dead simple logfile abstraction."

    def __init__(self, filename):
        self.filename = filename
        self.isLog = 1
        self.opened = 0
        self.fp = None

    def _open(self):
        if self.isLog and not self.opened:
            self.fp = open(self.filename, 'a')
            self.opened = 1

    def setLogging(self, level):
        self.isLog = level

    def getFile(self):
        return self.fp

    def write(self, msg):
        if self.isLog:
            self._open()
            self.fp.write(msg + "\n")

    def flush(self):
        self.fp.flush()

    def close(self):
        if self.fp:
            self.fp.close()
            self.fp = None

