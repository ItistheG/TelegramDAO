import org.javagram.dao.*;

import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

/**
 * Created by HerrSergio on 19.05.2016.
 */
public class Loader2 {
    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        try(TelegramDAO dao = new ApiBridgeTelegramDAO()) {
            dao.acceptNumberAndSendCode("79876497774");
            String code = scanner.nextLine();
            Me me = dao.signIn(code);

            TelegramProxy tlProxy = new TelegramProxy(dao);

            tlProxy.addObserver(new Observer() {
                @Override
                public void update(Observable observable, Object o) {
                    if(o == observable)
                        System.out.println("Heavy update");
                    else if(o instanceof List) {
                        System.out.println("New dialogs/contacts list");
                    } else if(o instanceof Person) {
                        System.out.println("Deleted/added/changed contact");
                    } else if(o instanceof Dialog) {
                        System.out.println("New messages AND/OR removed messages");
                    } else if(o == null) {
                        System.out.println("R.I.P. dialog");
                    } else {
                        System.out.println("WTF?");
                    }
                }
            });

            for(Person person : tlProxy.getPersons()) {
                for(Message message : tlProxy.getMessages(person, 5)) {
                    System.out.println(message.getText());
                }
            }

            for(int i = 0; i < 1; i++){
                Thread.sleep(3000);
                tlProxy.update();
            }

            for(Person person : tlProxy.getPersons()) {
                for(Message message : tlProxy.getMessages(person, 25)) {
                    System.out.println(message.getText());
                }
            }

            System.exit(0);
        }
    }
}
