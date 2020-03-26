package lsfusion.server.physics.admin.authentication.security.policy;

public class PropertySecurityPolicy {

    public ViewPropertySecurityPolicy view = new ViewPropertySecurityPolicy();
    public ChangePropertySecurityPolicy change = new ChangePropertySecurityPolicy();

    public void override(PropertySecurityPolicy policy) {
        view.override(policy.view);
        change.override(policy.change);
    }
}
