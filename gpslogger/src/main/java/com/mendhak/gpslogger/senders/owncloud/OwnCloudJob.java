package com.mendhak.gpslogger.senders.owncloud;



import android.net.Uri;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.owncloud.android.lib.common.*;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import org.slf4j.LoggerFactory;

import java.io.File;

import de.greenrobot.event.EventBus;


import com.owncloud.android.lib.common.OwnCloudClient;

public class OwnCloudJob extends Job implements OnRemoteOperationListener {

    private static final org.slf4j.Logger tracer = LoggerFactory.getLogger(OwnCloudJob.class.getSimpleName());


    String servername;
    String username;
    String password;
    String directory;
    File localFile;
    String remoteFileName;

    protected OwnCloudJob(String servername, String username, String password, String directory,
                         File localFile, String remoteFileName)
    {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(localFile)));
        this.servername = servername;
        this.username = username;
        this.password = password;
        this.directory = directory;
        this.localFile = localFile;
        this.remoteFileName = remoteFileName;

    }

    @Override
    public void onAdded() {
        tracer.debug("ownCloud Job: onAdded");
    }

    @Override
    public void onRun() throws Throwable {

        tracer.debug("ownCloud Job: Uploading  '"+localFile.getName()+"'");

        OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(servername), AppSettings.getInstance(), true);
        client.setDefaultTimeouts('\uea60', '\uea60');
        client.setFollowRedirects(true);
        client.setCredentials(
                OwnCloudCredentialsFactory.newBasicCredentials(username, password)
        );

        String remotePath = directory + FileUtils.PATH_SEPARATOR + localFile.getName();
        String mimeType = "application/octet-stream"; //unused
        UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(localFile.getAbsolutePath(), remotePath, mimeType);
        uploadOperation.execute(client,this);
    }

    @Override
    protected void onCancel() {

        tracer.debug("ownCloud Job: onCancel");
        EventBus.getDefault().post(new UploadEvents.OwnCloud(false));
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        tracer.error("Could not upload to OwnCloud", throwable);
        return false;
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation remoteOperation, RemoteOperationResult result) {

        if (!result.isSuccess()) {
            tracer.error(result.getLogMessage(), result.getException());
            EventBus.getDefault().post(new UploadEvents.OwnCloud(false));
        } else  {
            EventBus.getDefault().post(new UploadEvents.OwnCloud(true));
        }

        tracer.debug("ownCloud Job: onRun finished");
    }

    public static String getJobTag(File gpxFile) {
        return "OWNCLOUD" + gpxFile.getName();
    }
}