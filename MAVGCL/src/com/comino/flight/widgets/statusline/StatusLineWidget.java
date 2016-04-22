/****************************************************************************
 *
 *   Copyright (c) 2016 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.widgets.statusline;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.flight.widgets.charts.control.IChartControl;
import com.comino.flight.widgets.messages.MessagesWidget;
import com.comino.mav.control.IMAVController;
import com.comino.model.file.FileHandler;
import com.comino.msp.model.DataModel;
import com.comino.msp.model.segment.Status;
import com.comino.msp.utils.ExecutorService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;

public class StatusLineWidget extends Pane implements IChartControl  {

	@FXML
	private Label version;

	@FXML
	private Label driver;

	@FXML
	private Label messages;

	@FXML
	private Label elapsedtime;

	@FXML
	private Label currenttime;

	@FXML
	private Label sitl;

	@FXML
	private Label filename;

	private Task<Long> task;
	private IMAVController control;


	private final SimpleDateFormat fo = new SimpleDateFormat("mmm:ss");
	private FloatProperty scroll       = new SimpleFloatProperty(0);


	public StatusLineWidget() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("StatusLineWidget.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

		task = new Task<Long>() {

			List<DataModel> list = null;

			@Override
			protected Long call() throws Exception {
				while(true) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException iex) {
						Thread.currentThread().interrupt();
					}

					if(isDisabled()) {
						continue;
					}

					if (isCancelled()) {
						break;
					}
					Platform.runLater(() -> {

						if(control.getCurrentModel().sys.isStatus(Status.MSP_CONNECTED)) {
							driver.setText(control.getCurrentModel().sys.getSensorString());
							if(control.getMessageList().size()>0)
								messages.setText(control.getMessageList().remove(0).msg);
						} else
							driver.setText("not connected");

						list = control.getCollector().getModelList();

						if(list.size()>0) {
							int current_x0_pt = control.getCollector().calculateX0Index(scroll.floatValue());
							int current_x1_pt = control.getCollector().calculateX1Index(scroll.floatValue());
							elapsedtime.setText("TimeFrame: "+fo.format(list.get(current_x0_pt).tms/1000));
							currenttime.setText(fo.format(list.get(current_x1_pt).tms/1000));
						} else {
							elapsedtime.setText("TimeFrame: "+fo.format(list.get(0).tms/1000));
							currenttime.setText(fo.format(list.get(0).tms/1000));

						}

						if(control.isSimulation())
							sitl.setText("SITL");

						filename.setText(FileHandler.getInstance().getName());


					});
				}
				return System.currentTimeMillis();
			}
		};

		messages.setTooltip(new Tooltip("Click to show messagee"));
	}

	public void setup(ChartControlWidget chartControlWidget, IMAVController control) {
		chartControlWidget.addChart(this);
		this.control = control;
		messages.setText(control.getClass().getSimpleName()+ " loaded");
		ExecutorService.get().execute(task);
	}

	@Override
	public FloatProperty getScrollProperty() {
		return scroll;
	}

	public void registerMessageWidget(MessagesWidget m) {
		messages.setOnMousePressed(value -> {
			m.showMessages();
		});
	}

	@Override
	public BooleanProperty getCollectingProperty() {

		return null;
	}

	@Override
	public IntegerProperty getTimeFrameProperty() {

		return null;
	}

	@Override
	public void refreshChart() {


	}

}
