package services;

import entities.User;

public class CurrentUserService {

    public User getCurrentUser() {
        return UserService.getCurrentUser();
    }

    public int getCurrentUserId() {
        return getRequiredCurrentUser().getId();
    }

    public boolean isCurrentUserManager() {
        return getRequiredCurrentUser().isManager();
    }

    private User getRequiredCurrentUser() {
        User user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No authenticated current user is available.");
        }
        return user;
    }
}
