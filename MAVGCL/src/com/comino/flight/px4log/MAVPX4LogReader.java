package com.comino.flight.px4log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_log_data;
import org.mavlink.messages.lquac.msg_log_entry;
import org.mavlink.messages.lquac.msg_log_request_data;
import org.mavlink.messages.lquac.msg_log_request_end;
import org.mavlink.messages.lquac.msg_log_request_list;

import com.comino.mav.control.IMAVController;
import com.comino.model.file.FileHandler;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.main.control.listener.IMAVLinkListener;
import com.comino.msp.model.DataModel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import me.drton.jmavlib.log.px4.PX4LogReader;

public class MAVPX4LogReader implements IMAVLinkListener {

	private IMAVController control = null;
	private int     last_log_id   = 0;
	private long  log_bytes_read  = 0;
	private long  log_bytes_total = 0;

	private File tmpfile = null;
	private BufferedOutputStream out = null;

	private BooleanProperty isCollecting = new SimpleBooleanProperty();

	private long tms = 0;

	public MAVPX4LogReader(IMAVController control) {
		this.control = control;
		this.control.addMAVLinkListener(this);

		try {
			this.tmpfile = FileHandler.getInstance().getTempFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void requestLastLog() {
		isCollecting.set(true);
		msg_log_request_list msg = new msg_log_request_list(255,1);
		msg.target_component = 1;
		msg.target_system = 1;
		control.sendMAVLinkMessage(msg);
	}

	@Override
	public void received(Object o) {

		if( o instanceof msg_log_entry) {
			msg_log_entry entry = (msg_log_entry) o;
			last_log_id = entry.num_logs - 1;

			if(last_log_id > -1) {
				if(entry.id != last_log_id) {
					msg_log_request_list msg = new msg_log_request_list(255,1);
					msg.target_component = 1;
					msg.target_system = 1;
					msg.start= last_log_id;
					msg.end = last_log_id;
					control.sendMAVLinkMessage(msg);
				}
				else {
					try {
						out = new BufferedOutputStream(new FileOutputStream(tmpfile));
					} catch (FileNotFoundException e) { e.printStackTrace(); }
					log_bytes_read = 0; log_bytes_total = entry.size;
					MSPLogger.getInstance().writeLocalMsg("Get data from Log "+last_log_id+" Size: "+entry.size);
					msg_log_request_data msg = new msg_log_request_data(255,1);
					msg.target_component = 1;
					msg.target_system = 1;
					msg.id = last_log_id;
					msg.count = Long.MAX_VALUE;
					control.sendMAVLinkMessage(msg);
				}
			}
		}

		if( o instanceof msg_log_data) {
			msg_log_data data = (msg_log_data) o;

			for(int i=0;i< data.count;i++) {
				try {
					out.write(data.data[i]);
				} catch (IOException e) { e.printStackTrace(); }
			}
			log_bytes_read = data.ofs;

			if((System.currentTimeMillis()-tms)>5000) {
				MSPLogger.getInstance().writeLocalMsg("Loading px4log from device: "+getProgress()+"%",
						MAV_SEVERITY.MAV_SEVERITY_DEBUG);
				tms = System.currentTimeMillis();
			}

			if(data.count < 90) {
				try {
					out.close();
				} catch (IOException e) { e.printStackTrace();  }
				try {

					msg_log_request_end msg = new msg_log_request_end(255,1);
					msg.target_component = 1;
					msg.target_system = 1;
					control.sendMAVLinkMessage(msg);


					ArrayList<DataModel>modelList = new ArrayList<DataModel>();
					PX4LogReader reader = new PX4LogReader(tmpfile.getAbsolutePath());
					PX4toModelConverter converter = new PX4toModelConverter(reader,modelList);
					converter.doConversion();
					control.getCollector().setModelList(modelList);
					MSPLogger.getInstance().writeLocalMsg("Reading log from device finished");
				} catch (Exception e) { e.printStackTrace(); }

				isCollecting.set(false);;
			}
		}
	}

	public int getProgress() {
		return (int)((log_bytes_read * 100) / log_bytes_total);
	}

	public BooleanProperty isCollecting() {
		return isCollecting;
	}

}
