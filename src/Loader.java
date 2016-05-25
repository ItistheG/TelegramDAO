import org.javagram.dao.*;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by HerrSergio on 06.05.2016.
 */
public class Loader {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        try(TelegramDAO dao = new
                //DebugTelegramDAO()) {
                ApiBridgeTelegramDAO(Config.SERVER, Config.APP_ID, Config.APP_HASH)) {
            dao.acceptNumberAndSendCode(Config.PHONE_NUMBER);
            String code = scanner.nextLine();
            Me me = dao.signIn(code);
            System.out.println(me);

            State firstState = dao.getState();

            for (Contact contact : dao.getContacts()) {
                System.out.println(contact);
            }
            for(Dialog dialog : dao.getDialogs()) {
                System.out.println("Dialog with\n" + dialog.getBuddy());
                for(Message message : dao.getMessages(dialog.getBuddy(), null, 200)) {
                    System.out.println(message.getDate());
                    System.out.println("From :\n" + message.getSender());
                    System.out.println("To :\n" + message.getReceiver());
                    System.out.println(message.getText());
                }
            }

            for(Map.Entry<Person, Dialog> entry : dao.getList(false, false).entrySet()) {
                System.out.println(entry.getKey());
                System.out.println(entry.getValue());
            }

            State lastState = dao.getState();

            Updates updates = dao.getUpdates(firstState);
            System.out.println(updates.getState().isTheSameAs(firstState));
        } catch (Exception e) {
            System.exit(-1);
        }


        System.exit(0);
    }
}
