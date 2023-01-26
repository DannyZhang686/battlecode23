package pqual.irc;

public class IrcMessage {
    private final IrcEvent event;
    private final int data;
    private final int[] frag;

    public IrcMessage(IrcEvent _event, int _data, int[] _frag) {
        event = _event;
        data = _data;
        frag = _frag;
    }

    public IrcEvent getEvent() {
        return event;
    }

    public int getData() {
        return data;
    }

    public int[] getFrag() {
        return frag;
    }

    public boolean hasFrag() {
        return frag != null;
    }
}
