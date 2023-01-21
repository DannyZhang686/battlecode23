package rsprint2.irc;

public class IrcUtils {
    public static int getNextBufferHead(int head) {
        return (head == IrcConstants.IRC_BUFFER_END - 1) ? IrcConstants.IRC_BUFFER_START : head + 1;
    }

    public static int getNthBufferHead(int head, int n) {
        assert n < IrcConstants.IRC_BUFFER_LEN;

        head += n;

        if (head >= IrcConstants.IRC_BUFFER_END) {
            return head - IrcConstants.IRC_BUFFER_LEN;
        }

        return head;
    }
}