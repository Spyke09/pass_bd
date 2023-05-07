import java.io.Serializable;

public final class RegistrationPackage extends Package implements Serializable {
    private final String login;
    private final String password;
    private final String phoneNumber;
    private final String email;

    RegistrationPackage(String loggin, String password, String phoneNumber, String email) {
        super(PackageType.REGISTRATION);

        this.login = loggin;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return (
                super.toString() + ' ' +
                        this.login + ' ' +
                        this.password + ' ' +
                        this.phoneNumber + ' ' +
                        this.email
        );
    }
}
