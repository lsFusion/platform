package lsfusion.client.form.object.table;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.design.ClientComponent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientToolbar extends ClientComponent {

    public boolean visible = true;

    public boolean showGroupChange = true; //todo: убрать, когда все перейдут на версию 4 и уйдут все showGroupChange
    public boolean showCountRows = true;
    public boolean showCalculateSum = true;
    public boolean showGroupReport = true;
    public boolean showPrint = true; //todo: убрать, когда все перейдут на версию 4 и уйдут все showPrintGroup
    public boolean showXls = true;
    public boolean showSettings = true;

    public ClientToolbar() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        super.customSerialize(pool, outStream);

        outStream.writeBoolean(visible);

        outStream.writeBoolean(showGroupChange);
        outStream.writeBoolean(showCountRows);
        outStream.writeBoolean(showCalculateSum);
        outStream.writeBoolean(showGroupReport);
        outStream.writeBoolean(showPrint);
        outStream.writeBoolean(showXls);
        outStream.writeBoolean(showSettings);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        visible = inStream.readBoolean();

        showGroupChange = inStream.readBoolean();
        showCountRows = inStream.readBoolean();
        showCalculateSum = inStream.readBoolean();
        showGroupReport = inStream.readBoolean();
        showPrint = inStream.readBoolean();
        showXls = inStream.readBoolean();
        showSettings = inStream.readBoolean();
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        updateDependency(this, "visible");
    }

    public boolean getShowPrint() {
        return showPrint;
    }

    public void setShowPrint(boolean showPrint) {
        this.showPrint = showPrint;
        updateDependency(this, "showPrint");
    }

    public boolean getShowXls() {
        return showXls;
    }

    public void setShowXls(boolean showXls) {
        this.showXls = showXls;
        updateDependency(this, "showXls");
    }

    public boolean getShowSettings() {
        return showSettings;
    }

    public void setShowSettings(boolean showSettings) {
        this.showSettings = showSettings;
        updateDependency(this, "showSettings");
    }

    public boolean getShowCountRows() {
        return showCountRows;
    }

    public void setShowCountRows(boolean showCountRows) {
        this.showCountRows = showCountRows;
        updateDependency(this, "showCountRows");
    }

    public boolean getShowGroupChange() {
        return showGroupChange;
    }

    public void setShowGroupChange(boolean showGroupChange) {
        this.showGroupChange = showGroupChange;
        updateDependency(this, "showGroupChange");
    }

    public boolean getShowCalculateSum() {
        return showCalculateSum;
    }

    public void setShowCalculateSum(boolean showCalculateSum) {
        this.showCalculateSum = showCalculateSum;
        updateDependency(this, "showCalculateSum");
    }

    public boolean getShowGroupReport() {
        return showGroupReport;
    }

    public void setShowGroupReport(boolean showGroupButton) {
        this.showGroupReport = showGroupButton;
        updateDependency(this, "showGroupReport");
    }

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("logics.toolbar");
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.toolbar") + "[sid:" + getSID() + "]";
    }
}
