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

package com.comino.flight.tabs.xtanalysis;

import java.io.IOException;

import com.comino.flight.widgets.charts.control.ChartControlWidget;
import com.comino.flight.widgets.charts.line.LineChartWidget;
import com.comino.mav.control.IMAVController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

public class FlightXtAnalysisTab extends Pane {


	@FXML
	private LineChartWidget chart1;

	@FXML
	private LineChartWidget chart2;

//	@FXML
//	private LineChartWidget chart3;


	public FlightXtAnalysisTab() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FlightXtAnalysisTab.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {

			throw new RuntimeException(exception);
		}

	}

	@FXML
	private void initialize() {
		chart1.disableProperty().bind(this.disabledProperty());
		chart2.disableProperty().bind(this.disabledProperty());
//		chart3.disableProperty().bind(this.disabledProperty());
	}



	public void setup(ChartControlWidget recordControl,IMAVController control) {

		chart1.prefWidthProperty().bind(widthProperty());
		chart2.prefWidthProperty().bind(widthProperty());
//		chart3.prefWidthProperty().bind(widthProperty());

		chart1.prefHeightProperty().bind(heightProperty().divide(2).subtract(2));
		chart2.prefHeightProperty().bind(heightProperty().divide(2).subtract(2));
//		chart3.prefHeightProperty().bind(heightProperty().divide(3).subtract(2));

		recordControl.addChart(chart1.setup(control));
		recordControl.addChart(chart2.setup(control));
//		recordControl.addChart(chart3.setup(control));




	}



}
