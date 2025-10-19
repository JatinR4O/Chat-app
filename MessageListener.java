/**
 * Simple listener for server-side messages (logs, broadcasts) so GUIs can subscribe.
 */
public interface MessageListener {
    void onMessage(String message);
}
