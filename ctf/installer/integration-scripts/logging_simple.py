import time
import sys

DEBUG = "DEBUG"
INFO = "INFO "
WARNING = "WARN "
ERROR = "ERROR"

CRITICAL = 50
FATAL = CRITICAL
ERROR = 40
WARNING = 30
WARN = WARNING
INFO = 20
DEBUG = 10
NOTSET = 0

_levelNames = {
    CRITICAL : 'CRITICAL',
    ERROR : 'ERROR',
    WARNING : 'WARNING',
    INFO : 'INFO',
    DEBUG : 'DEBUG',
    NOTSET : 'NOTSET',
    'CRITICAL' : CRITICAL,
    'ERROR' : ERROR,
    'WARN' : WARNING,
    'WARNING' : WARNING,
    'INFO' : INFO,
    'DEBUG' : DEBUG,
    'NOTSET' : NOTSET,
}

class FakeLogger:

    def __init__(self):
        self.log = sys.stdout
        self.level = INFO

    def format(self, level, msg):
        t = time.time()
        milis = (".%.3f " % (t - int(t)))[1:]
        return time.strftime("%Y-%m-%d %H:%M:%S") + milis + _levelNames[level] + " " + msg
    
    def msg(self, level, msg):
        if level >= self.level:
            self.log.write(self.format(level, msg) + "\n")
            self.log.flush()
        
    def debug(self, msg, *args):
        if self.level == DEBUG:
            m = msg % args
            self.msg(DEBUG, m)

    def info(self, msg, *args):
        m = msg % tuple(args)
        self.msg(INFO, m)

    def exception(self, msg, e):
        self.msg(ERROR, msg + " " + str(e))

    def addHandler(self, h):
        self.log = open(h.fname, "a")

    def setLevel(self, level):
        self.level = level


class Formatter:
    def __init__(self, *arg1, **arg2):
        pass

class FileHandler:
    def __init__(self, fname):
        self.fname = fname

    def setFormatter(self, f):
        pass

def getLogger(name = None):
    return FakeLogger()

root = FakeLogger()

def debug(msg, *args):
    a = (msg,) + args
    root.debug(*a)

def info(msg, *args):
    a = (msg,) + args
    root.info(*a)

def exception(msg, e = None):
    if e == None:
        e = sys.exc_info()[1]
    root.exception(msg, e)

if __name__ == "__main__":
    root.setLevel(DEBUG)
    debug("foo %d %d", 4, 5)
    try:
        1 / 0
    except:
        exception("foo")
