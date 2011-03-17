package platform.interop.form;

import platform.interop.ClassViewType;
import platform.interop.action.ClientApply;
import platform.interop.remote.PendingRemote;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteFormInterface extends PendingRemote {

    // операции с ответом
    boolean hasCustomReportDesign() throws RemoteException;
    byte[] getRichDesignByteArray() throws RemoteException;

    byte[] getReportHierarchyByteArray() throws RemoteException;
    byte[] getReportDesignsByteArray(boolean toExcel) throws RemoteException;
    byte[] getReportSourcesByteArray() throws RemoteException;

    public RemoteChanges getRemoteChanges() throws RemoteException;

    String getSID() throws RemoteException;

    // синхронная проверка на то можно ли менять свойство
    byte[] getPropertyChangeType(int propertyID) throws RemoteException;
    boolean canChangeClass(int objectID) throws RemoteException;

    boolean hasClientApply() throws RemoteException; // чисто для оптимизации одного RMI вызова

    RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException;

    RemoteDialogInterface createObjectEditorDialog(int viewID) throws RemoteException;

    RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException;

    // операции без ответа, можно pendiть до первой операции с ответом

    void changePageSize(int groupID, Integer pageSize) throws RemoteException;
    void gainedFocus() throws RemoteException;

    void changeGroupObject(int groupID, byte[] value) throws RemoteException;

    void changeGroupObject(int groupID, byte changeType) throws RemoteException;

    void changePropertyDraw(int propertyID, byte[] object, boolean all, byte[] columnKeys) throws RemoteException;

    void switchClassView(int groupID) throws RemoteException;

    void changeClassView(int groupID, ClassViewType classView) throws RemoteException;

    void changeGridClass(int objectID,int idClass) throws RemoteException;

    void changePropertyOrder(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException;

    void clearUserFilters() throws RemoteException;

    void addFilter(byte[] state) throws RemoteException;

    void setRegularFilter(int groupID, int filterID) throws RemoteException;

    int countRecords(int groupObjectID) throws RemoteException;

    Object calculateSum(int propertyID, byte[] columnKeys) throws RemoteException;

    Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap) throws RemoteException;

    void refreshData() throws RemoteException;

    void cancelChanges() throws RemoteException;

    void expandGroupObject(int groupId, byte[] bytes) throws RemoteException;

    void moveGroupObject(int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException;

    ClientApply checkClientChanges() throws RemoteException;
    void applyClientChanges(Object clientResult) throws RemoteException;

    void applyChanges() throws RemoteException;

    void continueAutoActions() throws RemoteException;
}
