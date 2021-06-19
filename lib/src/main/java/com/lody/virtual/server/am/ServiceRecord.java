package com.lody.virtual.server.am;

import android.app.IServiceConnection;
import android.app.Notification;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServiceRecord extends Binder {
    final List<IntentBindRecord> bindings = new ArrayList<>();
    long activeSince;
    long lastActivityTime;
    ServiceInfo serviceInfo;
    int startId;
    ProcessRecord process;
    int foregroundId;
    Notification foregroundNoti;

    public boolean containConnection(IServiceConnection connection) {
        synchronized (bindings) {
            for (IntentBindRecord record : bindings) {
                if (record.containConnectionLocked(connection)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getClientCount() {
        synchronized (bindings) {
            return bindings.size();
        }
    }


    int getConnectionCountLocked() {
        int count = 0;
        for (IntentBindRecord record : bindings) {
            count += record.getConnectionCount();
        }
        return count;
    }


    IntentBindRecord peekBindingLocked(Intent service) {
        for (IntentBindRecord bindRecord : bindings) {
            if (bindRecord.intent.filterEquals(service)) {
                return bindRecord;
            }
        }
        return null;
    }

    void addToBoundIntentLocked(Intent intent, IServiceConnection connection) {
        IntentBindRecord record = peekBindingLocked(intent);
        if (record == null) {
            record = new IntentBindRecord();
            record.intent = intent;
            bindings.add(record);
        }
        record.addConnectionLocked(connection);
    }

    static class IntentBindRecord {
        final List<IServiceConnection> connections = new ArrayList<>();
        IBinder binder;
        Intent intent;
        boolean doRebind = false;

        public boolean containConnectionLocked(IServiceConnection connection) {
            for (IServiceConnection con : connections) {
                if (con.asBinder() == connection.asBinder()) {
                    return true;
                }
            }
            return false;
        }

        void addConnectionLocked(IServiceConnection connection) {
            if (!containConnectionLocked(connection)) {
                connections.add(connection);
                try {
                    connection.asBinder().linkToDeath(new DeathRecipient(this, connection), 0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        void removeConnectionLocked(IServiceConnection connection) {
            Iterator<IServiceConnection> iterator = connections.iterator();
            while (iterator.hasNext()) {
                IServiceConnection conn = iterator.next();
                if (conn.asBinder() == connection.asBinder()) {
                    iterator.remove();
                }
            }
        }

        int getConnectionCount() {
            synchronized (connections) {
                return connections.size();
            }
        }
    }

    private static class DeathRecipient implements IBinder.DeathRecipient {

        private final IntentBindRecord bindRecord;
        private final IServiceConnection connection;

        private DeathRecipient(IntentBindRecord bindRecord, IServiceConnection connection) {
            this.bindRecord = bindRecord;
            this.connection = connection;
        }

        @Override
        public void binderDied() {
            synchronized (bindRecord.connections) {
                bindRecord.removeConnectionLocked(connection);
            }
            connection.asBinder().unlinkToDeath(this, 0);
        }
    }

}