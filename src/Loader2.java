import org.javagram.dao.*;
import org.javagram.dao.proxy.TelegramProxy;
import org.javagram.dao.proxy.changes.UpdateChanges;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
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
        try(TelegramDAO dao = new ApiBridgeTelegramDAO(Config.SERVER, Config.APP_ID, Config.APP_HASH)) {
            dao.acceptNumberAndSendCode(Config.PHONE_NUMBER);
            String code = scanner.nextLine();
            Me me = dao.signIn(code);

            TelegramProxy tlProxy = new TelegramProxy(dao);

            BufferedImage myImage = tlProxy.getPhoto(me, true);
            boolean amOnline = tlProxy.isOnline(me);

            tlProxy.addObserver(new Observer() {
                @Override
                public void update(Observable observable, Object o) {
                    if(o instanceof UpdateChanges) {
                        UpdateChanges updateChanges = (UpdateChanges) o;
                        System.out.println(updateChanges.getListChanged());
                        for(Dialog dialog : updateChanges.getDialogsToReset()) {
                            /*List<Message> messages = updateChanges.getNewMessages().get(dialog);
                            for(Message message : messages) {
                                System.out.println(message.getText());
                            }*/
                            System.out.println(dialog.getBuddy());
                        }
                    }
                }
            });

            for(Person person : tlProxy.getPersons()) {
                //savePhoto(tlProxy, person);
                for(Message message : tlProxy.getMessages(person, 5)) {
                    System.out.println(message.getText());
                }
                System.out.println("Online : " + tlProxy.onlineUntil(person));
            }

            for(int i = 0; i < 1; i++){
                Thread.sleep(3000);
                tlProxy.update();
            }

            for(Person person : tlProxy.getPersons()) {
                //savePhoto(tlProxy, person);
                for(Message message : tlProxy.getMessages(person, 25)) {
                    System.out.println(message.getText());
                }
            }

            System.exit(0);
        }

    }

    private static void savePhoto(TelegramProxy tlProxy, Person person) throws IOException {
        savePhoto(tlProxy, person, true);
        savePhoto(tlProxy, person, false);
    }

    private static void savePhoto(TelegramProxy tlProxy, Person person, boolean small) throws IOException {
        BufferedImage bi = tlProxy.getPhoto(person, small);
        String ext = "png";
        String fileName = small ? "small" : "large";
        fileName += person.getId();
        fileName += "." + ext;
        if(bi != null) {
            ImageIO.write(bi, ext, new File(fileName));
        }
    }
}
