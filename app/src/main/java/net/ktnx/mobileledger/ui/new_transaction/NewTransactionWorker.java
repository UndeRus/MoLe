package net.ktnx.mobileledger.ui.new_transaction;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;

import com.google.gson.Gson;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.SendTransactionImpl;
import net.ktnx.mobileledger.dao.ProfileDAO;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NewTransactionWorker extends Worker {

    private static final String CHANNEL_ID = "main_channel";
    private final NotificationManager notificationManager;

    public NewTransactionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Gson gson = new Gson();
        Data data = getInputData();
        String profileUUID = data.getString(KEY_PROFILE_UUID);
        LedgerTransaction transaction = gson.fromJson(data.getString (KEY_TRANSACTION), LedgerTransaction.class);
        boolean simulate = data.getBoolean(KEY_SIMULATE, false);
        int amount = 0;
        String currency = "";
        for(LedgerTransactionAccount account: transaction.getAccounts()) {
            if ( account.getAmount() > 0 ){
                amount += account.getAmount();
                currency = " " + account.getCurrency();
            }
        }
        setForegroundAsync(createForegroundInfo(transaction.getDescription() + " " + amount + currency));
        SendTransactionImpl sendTransaction = new SendTransactionImpl();
        ProfileDAO dao = DB.get().getProfileDAO();
        Profile profile = dao.getByUuidSync(profileUUID);
        sendTransaction.send(profile, transaction, simulate, (error, args) -> {});

        if (sendTransaction.getError() != null) {
            return Result.retry();
        } else {
            return Result.success(data);
        }
    }

    private ForegroundInfo createForegroundInfo(String title) {
        Context context = getApplicationContext();
        String id = context.getString(R.string.notification_channel_id);
        String cancel = context.getString(R.string.cancel_transaction);
        // This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        Notification notification = new NotificationCompat.Builder(context, id)
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(R.drawable.ic_work_notification)
                .setOngoing(true)
                .setProgress(0, 100, true)
                .setChannelId(CHANNEL_ID)
                // Add the cancel action to the notification which can
                // be used to cancel the worker
                .addAction(android.R.drawable.ic_delete, cancel, intent)
                .build();

        int notificationId = 8800;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            return new ForegroundInfo(notificationId, notification);
        }
    }

    private void createChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getApplicationContext().getString(R.string.channel_name);
            String description = getApplicationContext().getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static Data createInputData(Profile profile, LedgerTransaction transaction, boolean simulate) {
        Gson gson = new Gson();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(KEY_PROFILE_UUID, profile.getUuid());
        dataMap.put(KEY_TRANSACTION, gson.toJson(transaction));
        dataMap.put(KEY_SIMULATE, simulate);
        Data.Builder builder = new Data.Builder()
                .putAll(dataMap);

        return builder.build();
    }

    public static String KEY_PROFILE_UUID = "profile_uuid";
    public static String KEY_TRANSACTION = "transaction";
    public static String KEY_SIMULATE = "simulate";
    public static String KEY_ERROR = "error";
}
