/**
 * Copyright (C) 2013 Aurélien Chabot <aurelien@chabot.fr>
 * 
 * This file is part of DroidUPNP.
 * 
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http://www.gnu.org/licenses/>.
 */

package sc.music.upnp.cling;

import android.util.Log;


import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.support.avtransport.callback.GetMediaInfo;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.PlaylistItem;
import org.fourthline.cling.support.model.item.TextItem;
import org.fourthline.cling.support.model.item.VideoItem;
import org.fourthline.cling.support.renderingcontrol.callback.GetMute;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetMute;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;

import sc.music.Main;
import sc.music.upnp.cling.didl.ClingDIDLItem;
import sc.music.upnp.didl.IDIDLItem;
import sc.music.upnp.model.IRendererCommand;

@SuppressWarnings("rawtypes")
public class RendererCommand implements Runnable, IRendererCommand {

	private static final String TAG = "RendererCommand";

	private final RendererState rendererState;
	private final ControlPoint controlPoint;

	public Thread thread;
	boolean pause = false;

	//传入的cling的控制点啊
	public RendererCommand(ControlPoint controlPoint, RendererState rendererState)
	{
		this.rendererState = rendererState;
		this.controlPoint = controlPoint;

		//并且还创建了一个线程
		thread = new Thread(this);
		pause = true;
	}

	@Override
	public void finalize()
	{
		this.pause();
	}

	@Override
	public void pause()
	{
		Log.v(TAG, "Interrupt");
		pause = true;
		thread.interrupt();
	}

	@Override
	public void resume()
	{
		Log.v(TAG, "Resume");
		pause = false;
		if (!thread.isAlive())
			thread.start();
		else
			thread.interrupt();
	}

	public static Service getRenderingControlService()
	{
		if (Main.upnpServiceController.getSelectedRenderer() == null)
			return null;

		return ((CDevice) Main.upnpServiceController.getSelectedRenderer()).getDevice().findService(
				new UDAServiceType("RenderingControl"));
	}

	public static Service getAVTransportService()
	{
		if (Main.upnpServiceController.getSelectedRenderer() == null)
			return null;

		return ((CDevice) Main.upnpServiceController.getSelectedRenderer()).getDevice().findService(
				new UDAServiceType("AVTransport"));
	}

	@Override
	public void commandPlay()//播放
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new Play(getAVTransportService()) {
			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success playing ! ");
				// TODO update player state
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to play ! " + arg2);
			}
		});
	}

	@Override
	public void commandStop()//停止
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new Stop(getAVTransportService()) {
			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success stopping ! ");
				// TODO update player state
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to stop ! " + arg2);
			}
		});
	}

	@Override
	public void commandPause()//暂停
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new Pause(getAVTransportService()) {
			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success pausing ! ");
				// TODO update player state
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to pause ! " + arg2);
			}
		});
	}

	@Override
	public void commandToggle()//状态切换，play和pause的切换
	{
		RendererState.State state = rendererState.getState();
		if (state == RendererState.State.PLAY)
		{
			commandPause();
		}
		else
		{
			commandPlay();
		}
	}

	@Override
	public void commandSeek(String relativeTimeTarget)//跳转
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new Seek(getAVTransportService(), relativeTimeTarget) {
			// TODO fix it, what is relativeTimeTarget ? :)

			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success seeking !");
				// TODO update player state
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to seek ! " + arg2);
			}
		});
	}

	@Override
	public void setVolume(final int volume)//设置音量
	{
		if (getRenderingControlService() == null)
			return;

		controlPoint.execute(new SetVolume(getRenderingControlService(), volume) {
			@Override
			public void success(ActionInvocation invocation)
			{
				super.success(invocation);
				Log.v(TAG, "Success to set volume");
				rendererState.setVolume(volume);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to set volume ! " + arg2);
			}
		});
	}

	@Override
	public void setMute(final boolean mute)//静音
	{
		if (getRenderingControlService() == null)
			return;

		controlPoint.execute(new SetMute(getRenderingControlService(), mute) {
			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success setting mute status ! ");
				rendererState.setMute(mute);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to set mute status ! " + arg2);
			}
		});
	}

	@Override
	public void toggleMute()
	{
		setMute(!rendererState.isMute());
	}//切换静音

	//最后通过这里启动播放
	public void setURI(String uri, TrackMetadata trackMetadata)
	{
		Log.i(TAG, "Set uri to " + uri);

		//这是发给dmr了，从此开始控制dmr
		controlPoint.execute(new SetAVTransportURI(getAVTransportService(), uri, trackMetadata.getXML()) {

			@Override
			public void success(ActionInvocation invocation) {
				super.success(invocation);
				Log.i(TAG, "URI successfully set !");
				commandPlay();
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
				Log.w(TAG, "Fail to set URI ! " + arg2);
			}
		});
	}

	//首先调用这个
	@Override
	public void launchItem(final IDIDLItem item)
	{
		if (getAVTransportService() == null)
			return;

		DIDLObject obj = ((ClingDIDLItem) item).getObject();
		if (!(obj instanceof Item))
			return;

		Item upnpItem = (Item) obj;


		String type = "";
		if (upnpItem instanceof AudioItem)
			type = "audioItem";
		else if (upnpItem instanceof VideoItem)
			type = "videoItem";
		else if (upnpItem instanceof ImageItem)
			type = "imageItem";
		else if (upnpItem instanceof PlaylistItem)
			type = "playlistItem";
		else if (upnpItem instanceof TextItem)
			type = "textItem";

		if(type.equals("audioItem"))
		{
			Log.e(TAG,"audioItem ,check localpath");

		}
		else
			Log.e(TAG,"NOT audioItem");
		// TODO genre && artURI  专辑图片还不支持啊
		//通过upnpitem构造一个TrackMetadata出来，传递信息都来自item
		final TrackMetadata trackMetadata = new TrackMetadata(upnpItem.getId(), upnpItem.getTitle(),
				upnpItem.getCreator(), "", "", upnpItem.getFirstResource().getValue(),
				"object.item." + type);
		Log.e(TAG,"url : upnpItem.getFirstResource().getValue() : ["+upnpItem.getFirstResource().getValue()+"]");
		Log.i(TAG, "TrackMetadata : "+trackMetadata.toString());

		//重新播放之前，首先暂停
		// Stop playback before setting URI
		controlPoint.execute(new Stop(getAVTransportService()) {
			@Override
			public void success(ActionInvocation invocation)
			{
				Log.v(TAG, "Success stopping ! ");
				callback();
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to stop ! " + arg2);
				callback();
			}

			public void callback()
			{
				//原来在这里，使用这俩来给播放器播放
				/* 果然是全路径的
				07-10 17:20:26.782: E/RendererCommand(24220):
				 item.getURI()http://172.16.34.206:8192/a-8070.mp3]
				这个URL是http的啊
				* */
				Log.e(TAG,"item.getURI()"+item.getURI()+"]");
				setURI(item.getURI(), trackMetadata);
			}
		});

	}

	// Update

	public void updateMediaInfo()
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new GetMediaInfo(getAVTransportService()) {
			@Override
			public void received(ActionInvocation arg0, MediaInfo arg1)
			{
				Log.d(TAG, "Receive media info ! " + arg1);
				rendererState.setMediaInfo(arg1);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get media info ! " + arg2);
			}
		});
	}

	public void updatePositionInfo()//更新当前的位置
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new GetPositionInfo(getAVTransportService()) {
			@Override
			public void received(ActionInvocation arg0, PositionInfo arg1)
			{
				Log.d(TAG, "Receive position info ! " + arg1);
				rendererState.setPositionInfo(arg1);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get position info ! " + arg2);
			}
		});
	}

	public void updateTransportInfo()//更新传输信息
	{
		if (getAVTransportService() == null)
			return;

		controlPoint.execute(new GetTransportInfo(getAVTransportService()) {
			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get position info ! " + arg2);
			}

			@Override
			public void received(ActionInvocation arg0, TransportInfo arg1)
			{
				Log.d(TAG, "Receive position info ! " + arg1);
				rendererState.setTransportInfo(arg1);
			}
		});
	}

	@Override
	public void updateVolume()//更新音量
	{
		if (getRenderingControlService() == null)
			return;

		controlPoint.execute(new GetVolume(getRenderingControlService()) {
			@Override
			public void received(ActionInvocation arg0, int arg1)
			{
				Log.d(TAG, "Receive volume ! " + arg1);
				rendererState.setVolume(arg1);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get volume ! " + arg2);
			}
		});
	}

	public void updateMute()
	{
		if (getRenderingControlService() == null)
			return;

		controlPoint.execute(new GetMute(getRenderingControlService()) {
			@Override
			public void received(ActionInvocation arg0, boolean arg1)
			{
				Log.d(TAG, "Receive mute status ! " + arg1);
				rendererState.setMute(arg1);
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2)
			{
				Log.w(TAG, "Fail to get mute status ! " + arg2);
			}
		});
	}

	@Override
	public void updateFull()
	{
		updateMediaInfo();
		updatePositionInfo();
		updateVolume();
		updateMute();
		updateTransportInfo();
	}

	@Override
	public void run()
	{
		// LastChange lastChange = new LastChange(new AVTransportLastChangeParser(),
		// AVTransportVariable.CurrentTrackMetaData.class);

		// SubscriptionCallback callback = new SubscriptionCallback(getRenderingControlService(), 600) {
		//
		// @Override
		// public void established(GENASubscription sub)
		// {
		// Log.e(TAG, "Established: " + sub.getSubscriptionId());
		// }
		//
		// @Override
		// public void failed(GENASubscription sub, UpnpResponse response, Exception ex, String msg)
		// {
		// Log.e(TAG, createDefaultFailureMessage(response, ex));
		// }
		//
		// @Override
		// public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response)
		// {
		// // Reason should be null, or it didn't end regularly
		// }
		//
		// @Override
		// public void eventReceived(GENASubscription sub)
		// {
		// Log.e(TAG, "Event: " + sub.getCurrentSequence().getValue());
		// Map<String, StateVariableValue> values = sub.getCurrentValues();
		// StateVariableValue status = values.get("Status");
		// if (status != null)
		// Log.e(TAG, "Status is: " + status.toString());
		// }
		//
		// @Override
		// public void eventsMissed(GENASubscription sub, int numberOfMissedEvents)
		// {
		// Log.e(TAG, "Missed events: " + numberOfMissedEvents);
		// }
		// };

		// controlPoint.execute(callback);

		while (true)
			try
			{
				int count = 0;
				while (true)
				{
					if (!pause)
					{
						Log.d(TAG, "Update state !");

						count++;

						updatePositionInfo();

						if ((count % 3) == 0)
						{
							updateVolume();
							updateMute();
							updateTransportInfo();
						}

						if ((count % 6) == 0)
						{
							updateMediaInfo();
						}
					}
					Thread.sleep(1000);
				}
			}
			catch (InterruptedException e)
			{
				Log.i(TAG, "State updater interrupt, new state " + ((pause) ? "pause" : "running"));
			}
	}

	@Override
	public void updateStatus()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePosition()
	{
		// TODO Auto-generated method stub

	}
}
