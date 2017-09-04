/***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation. 
 * @author Mustansar@rice.edu
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.calendar.impl;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Vector;
import java.util.List;

import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeRange;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.time.api.TimeBreakdown;

import org.sakaiproject.util.CalendarUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
* Sunday, Monday, Wednesday recurrence rule
*/
public class SMWRecurrenceRule extends RecurrenceRuleBase
{
	
	protected final static String FREQ = "SMW";
	private CalendarUtil calUtil = null;
	
	public SMWRecurrenceRule() {
		super();
		calUtil = new CalendarUtil();
	}	
	
	public SMWRecurrenceRule(int interval) {
		super(interval);
		calUtil = new CalendarUtil();
	}	

	
	public SMWRecurrenceRule(int interval, int count) {
		super(interval, count);
		calUtil = new CalendarUtil();
	}	
	
	public SMWRecurrenceRule(int interval, Time until) {
		super(interval, until);	
		calUtil = new CalendarUtil();
	}	
	
	public Element toXml(Document doc, Stack stack) {
		Element rule = doc.createElement("rule");
		((Element)stack.peek()).appendChild(rule);
		rule.setAttribute("class", "org.chefproject.osid.calendar.SMWRecurrenceRule");
		rule.setAttribute("name", "SMWRecurrenceRule");
		setBaseClassXML(rule);
		return rule;
	}

	
	protected int getRecurrenceType() {
		return GregorianCalendar.WEEK_OF_MONTH;	
	}
   
	/**
	 * {@inheritDoc}
	 */
	public String getFrequencyDescription() {
		return rb.getFormattedMessage("set.SMW.fm", calUtil.getDayOfWeekName(0), calUtil.getDayOfWeekName(1), calUtil.getDayOfWeekName(3));
	}
   
	
	public String getFrequency() {
		return FREQ;
	}
   
   
	
	/**
	* Return a List of all RecurrenceInstance objects generated by this rule within the given time range, based on the
	* prototype first range, in time order.
	* @param prototype The prototype first TimeRange.
	* @param range A time range to limit the generated ranges.
	* @param timeZone The time zone to use for displaying times.
	* %%% Note: this is currently not implemented, and always uses the "local" zone.
	* @return a List of RecurrenceInstance generated by this rule in this range.
	*/
	public List generateInstances(TimeRange prototype, TimeRange range, TimeZone timeZone)
	{
		TimeBreakdown startBreakdown = prototype.firstTime().breakdownLocal();			
		List rv = new Vector();
		GregorianCalendar startCalendarDate = TimeService.getCalendar(TimeService.getLocalTimeZone(),0,0,0,0,0,0,0);	
		startCalendarDate.set(
									 startBreakdown.getYear(),
									 startBreakdown.getMonth() - 1, 
									 startBreakdown.getDay(), 
									 startBreakdown.getHour(),	
									 startBreakdown.getMin(), 
									 startBreakdown.getSec());	 //may have to move this line ahead 
		
		GregorianCalendar nextCalendarDate = (GregorianCalendar) startCalendarDate.clone();	
		
		//if day of week is not Sunday, Monday or Wednesday
		if( ((startCalendarDate.get(GregorianCalendar.DAY_OF_WEEK)!=1) && (startCalendarDate.get(GregorianCalendar.DAY_OF_WEEK)!=2) && ((startCalendarDate.get(GregorianCalendar.DAY_OF_WEEK))!=4 ))){
			
			//if day of week is Tuesday, add one to make it Wednesday
			if (startCalendarDate.get(GregorianCalendar.DAY_OF_WEEK)==3){
				startCalendarDate.add(java.util.Calendar.DAY_OF_MONTH, 1);
			}
			//if day of week is Thursday, add three to make it next Sunday
			else if (startCalendarDate.get(GregorianCalendar.DAY_OF_WEEK)==5){
				startCalendarDate.add(java.util.Calendar.DAY_OF_MONTH, 3);
			} 
			//if day of week is Friday, add two to make it next Sunday
			else if (startCalendarDate.get(GregorianCalendar.DAY_OF_WEEK)==6){
				startCalendarDate.add(java.util.Calendar.DAY_OF_MONTH, 2);
			}
			//must be Saturday, add one to make it next Sunday
			else {
				startCalendarDate.add(java.util.Calendar.DAY_OF_MONTH, 1);			 
			} 
		}
		
		nextCalendarDate = (GregorianCalendar) startCalendarDate.clone();
		int currentCount = 1;
		int hitCount=1;
		do
		{	
			Time nextTime = TimeService.newTime(nextCalendarDate);
			// is this past count?
			if ((getCount() > 0) && (hitCount > getCount()))
				break; 
			// is this past until?
			if ((getUntil() != null) && isAfter(nextTime, getUntil()) )
				break;
			
			TimeRange nextTimeRange = TimeService.newTimeRange(nextTime.getTime(), prototype.duration());
			
			// Is this out of the range?
			if (isOverlap(range, nextTimeRange))
			{
				TimeRange eventTimeRange = null;
				
				// Single time cases require special handling.
				if ( prototype.isSingleTime() )
				{
					eventTimeRange = TimeService.newTimeRange(nextTimeRange.firstTime());
				}
				else
				{
					eventTimeRange = TimeService.newTimeRange(nextTimeRange.firstTime(), nextTimeRange.lastTime(), true, false);
				}
				
				// use this one
				String eventHR=eventTimeRange.toStringHR();
				rv.add(new RecurrenceInstance(eventTimeRange, currentCount));
			}
			
			// if next starts after the range, stop generating
			else if (isAfter(nextTime, range.lastTime()) ) { 
				break;
			}
         			
			// Examine every day in the calendar, if next date is not Sunday or Monday or Wednesday
			do{				
				int weekDay=nextCalendarDate.get(GregorianCalendar.DAY_OF_WEEK);
				
				//if we have past all applicable days for this week, skip the (interval weeks-1) + 1 day, then continue processing.
				//for example if this is a thursday and interval is two weeks, we skip to friday of next week then continue.
				if((getInterval()>1&&(weekDay==5)))
				{	
					int increment = (((getInterval()-1)*7)+1);
					
					nextCalendarDate.add(java.util.Calendar.DAY_OF_MONTH, increment);
					currentCount+=increment;
				}
				else 
				//check next day
				{
					nextCalendarDate.add(java.util.Calendar.DAY_OF_MONTH, 1);
					currentCount++;
				}
			} while((nextCalendarDate.get(GregorianCalendar.DAY_OF_WEEK)!=1)&&(nextCalendarDate.get(GregorianCalendar.DAY_OF_WEEK)!=2)&&((nextCalendarDate.get(GregorianCalendar.DAY_OF_WEEK))!=4));
			
			
			hitCount++;
		} while (true);
		
		return rv;
	}
}	



