import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

public class UserList extends VBox
{
    public UserList() {}

    public void addUser(String userName, ArrayList<Image> badges)
    {
        if (indexOfUsersHBox(userName) != null)
        {
            addBadgesToUser(userName, badges);
            return;
        }

        addUser(userName);
        addBadgesToUser(userName, badges);
    }

    public void addUser(String userName)
    {
        if (indexOfUsersHBox(userName) != null)
            return;

        HBox userContainter = new HBox();
        Label userNameLabel = new Label(userName);

        userContainter.getChildren().add(userNameLabel);
        userContainter.setSpacing(3.0);

        this.getChildren().add(userContainter);
    }

    public Integer indexOfUsersHBox(String userToSearchFor)
    {
        for (Node hbox : this.getChildren())
            if (hbox instanceof HBox)
                for (Node node : ((HBox) hbox).getChildren())
                    if (node instanceof Label)
                        if (((Label) node).getText().equals(userToSearchFor))
                            return this.getChildren().indexOf(hbox);

        return null;
    }

    public void addBadgesToUser(String userToSearchFor, ArrayList<Image> badges)
    {
        Integer indexOfUsersHBox;

        // find the users hbox and test if the user exists
        if ((indexOfUsersHBox = indexOfUsersHBox(userToSearchFor)) != null)
        {
            Label userName = null;
            HBox alreadyLoadedHBox;
            HBox newHBox = new HBox();
            newHBox.setSpacing(3.0);

            alreadyLoadedHBox = (HBox) this.getChildren().get(indexOfUsersHBox);

            // Reuse the users name
            for (Node node : alreadyLoadedHBox.getChildren())
            {
                if (node instanceof Label)
                {
                    userName = (Label) node;
                }
            }

            for (Image img : badges)
                newHBox.getChildren().add(new ImageView(img));

            if (userName == null)
                throw new IllegalArgumentException("Failed to find username despite finding it earlier");

            newHBox.getChildren().add(userName);

            this.getChildren().remove(indexOfUsersHBox.intValue());

            this.getChildren().add(indexOfUsersHBox, newHBox);
        }
        else
            addUser(userToSearchFor, badges);
    }

    public void removeUser(String userToSearchFor)
    {
        Integer index;
        if ((index = indexOfUsersHBox(userToSearchFor)) != null)
            this.getChildren().remove(index.intValue());
    }

    public void removeAllUsers() { this.getChildren().clear(); }
}
