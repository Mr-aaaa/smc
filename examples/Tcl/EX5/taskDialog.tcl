# 
# The contents of this file are subject to the Mozilla Public
# License Version 1.1 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.mozilla.org/MPL/
# 
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
# 
# The Original Code is State Machine Compiler (SMC).
# 
# The Initial Developer of the Original Code is Charles W. Rapp.
# Portions created by Charles W. Rapp are
# Copyright (C) 2000 Charles W. Rapp.
# All Rights Reserved.
# 
# Contributor(s):
#
# TaskDialog --
#
#  Displays the task dialog box and collects the task parameters.
#  On OK, sends a createTask message to the GUI controller.
#
# RCS ID
# $Id$
#
# CHANGE LOG
# $Log$
# Revision 1.3  2002/02/19 19:52:48  cwrapp
# Changes in release 1.3.0:
# Add the following features:
# + 479555: Added subroutine/method calls as argument types.
# + 508878: Added %import keyword.
#
# Revision 1.2  2001/05/09 23:40:02  cwrapp
# Changes in release 1.0, beta 6:
# Fixes the four following bugs:
# + 416011: SMC does not properly handle pop transitions which
#           have no argument.
# + 416013: SMC generated code does not throw a
#           "Transition Undefined" exception as per Programmer's
#           Manual.
# + 416014: The initial state's Entry actions are not being
#           executed.
# + 416015: When a transition has both a guarded and an unguarded
#           definition, the Exit actions are only called when the
#           guard evaluates to true.
# + 422795: SMC -tcl abnormally terminates.
#
# Revision 1.1.1.1  2001/01/03 03:14:00  cwrapp
#
# ----------------------------------------------------------------------
# SMC - The State Map Compiler
# Version: 1.0, Beta 3
#
# SMC compiles state map descriptions into a target object oriented
# language. Currently supported languages are: C++, Java and [incr Tcl].
# SMC finite state machines have such features as:
# + Entry/Exit actions for states.
# + Transition guards
# + Transition arguments
# + Push and Pop transitions.
# + Default transitions. 
# ----------------------------------------------------------------------
#
# Revision 1.1.1.1  2000/08/02 12:51:05  charlesr
# Initial source import, SMC v. 1.0, Beta 1.
#

class TaskCreateDialog {

# Member data.
    # Store the top frame widget here.
    private variable _topFrame;

    # Store the top frame's geometry here.
    private variable _geometry;

    # While the task dialog is being filled in,
    # store the task parameters here.
    private variable _name;
    private variable _priority;
    private variable _time;

# Member functions.
    # constructor --
    #
    #   Create the task dialog box and hide it.
    #
    # Arguments:
    #   top   The top frame containing the dialog widget.
    #
    # Results:
    #   None.

    constructor {} {
        ResetParameters;

        # Create the dialog GUI.
        set _topFrame [CreateDialog];
        set _geometry "";
    }

    destructor {}

    # setName --
    #
    #   Store away the task's name.
    #
    # Arguments:
    #   name   The task name.
    #
    # Results:
    #   None.

    public method setName {name} {
        set _name $name;
        return -code ok;
    }

    # setPriority --
    #
    #   Store away the task's priority.
    #
    # Arguments:
    #   priority   The task priority.
    #
    # Results:
    #   None.

    public method setPriority {priority} {
        set _priority $priority;
        return -code ok;
    }

    # setTime --
    #
    #   Store away the task's time.
    #
    # Arguments:
    #   time   The task time.
    #
    # Results:
    #   None.

    public method setTime {time} {
        set _time $time;
        return -code ok;
    }

    # display --
    #
    #   Display the CreateTask dialog. Check if the window
    #   already exists. If it does, unhide it and bring it
    #   to the foreground. If not, then create the window.
    #
    # Arguments:
    #   None.
    #
    # Results:
    #   None.

    public method display {} {
        global widget Gwait_for_it;
        variable OldFocus "";

        # If the dialog does not exist, then create it.
        # Otherwise, bring it out of hiding.
        if {[string length $_topFrame] == 0} {
            set _topFrame [CreateDialog];
        } else {
            wm deiconify $_topFrame;
        }

        if {[string length $_geometry] > 0} {
            catch {wm geometry $_topFrame $_geometry};
        }

        # Focus on the dialog's name entry field.
        set OldFocus [focus -displayof $_topFrame];
        focus $_topFrame.nameEntry;
        catch {tkwait visibility $_topFrame};
        catch {grab $_topFrame};

        # Wait for the dialog to complete. When done, reset the
        # focus to where it had been.
        set Gwait_for_it false;
        tkwait variable Gwait_for_it;
        catch {grab release $_topFrame};
        focus $OldFocus;

        # Check the dialog's completion. If true, then
        # the user set the variables and is trying to
        # create a task.
        if {[string compare $Gwait_for_it "true"] == 0} {
            guiController postMessage \
                    taskManager createTask $_name $_priority $_time;
        }

        # Reset the parameters because whether the task creation
        # was successful or not, they are no longer needed.
        ResetParameters;

        # If possible, save the dialog's current size and
        # position.
        if {[catch {set _geometry [wm geometry $_topFrame]; wm withdraw $_topFrame;}] != 0} {
            # The dialog box was destroyed. Create it again
            # the next time it is needed.
            set _topFrame "";
            set _geometry "";
        } else {
            $_topFrame.nameEntry delete 0 end;
            $_topFrame.priorityScale set $_priority;
            $_topFrame.timeScale set $_time;
        }

        return -code ok;
    }

    # ResetParameters --
    #
    #   Reset the task creation parameters to their uninitialized
    #   values.
    #
    # Arguments:
    #   None.
    #
    # Results:
    #   None.

    private method ResetParameters {} {
        set _name "";
        set _priority 0;
        set _time 0;

        return -code ok;

    }

    # CreateDialog --
    #
    #   Create the dialog box but keep it hidden.
    #
    # Arguments:
    #   None.
    #
    # Results:
    #   The name of the top frame.

    private method CreateDialog {} {
        set base .createTaskDialog;

        toplevel $base -class Toplevel -relief raised;
        wm withdraw $base;
        wm focusmodel $base passive;
        wm geometry $base 256x193+451+410;
        wm maxsize $base 1284 1009;
        wm minsize $base 104 1;
        wm overrideredirect $base 0;
        wm resizable $base 0 0;
        wm title $base "Create Task";

        # Handle Control-C and destroy events.
        bind $base <Control-Key-c> {
            global Gwait_for_it;
            set Gwait_for_it false;
            break;
        }
        bind $base <Destroy> {
            global Gwait_for_it;
            set Gwait_for_it false;
        }

        # Add the dialog widgets.
        label $base.nameLabel \
                -borderwidth 1 \
                -justify left \
                -text Name;
        entry $base.nameEntry \
                -textvariable ::TaskName;
        label $base.priorityLabel \
                -borderwidth 1 \
                -justify left \
                -text Priority;
        scale $base.priorityScale \
                -from 1.0 \
                -orient horizontal \
                -tickinterval 1.0 \
                -to 10.0 \
                -variable ::TaskPriority;
        label $base.timeLabel \
                -borderwidth 1 \
                -justify left \
                -text Time;
        scale $base.timeScale \
                -from 1.0 \
                -orient horizontal \
                -tickinterval 5.0 \
                -to 60.0 \
                -variable ::TaskTime;
        button $base.okButton \
                -command { \
                    global Gwait_for_it; \
                    taskCreateDialog setName ${::TaskName}; \
                    taskCreateDialog setPriority ${::TaskPriority}; \
                    taskCreateDialog setTime ${::TaskTime}; \
                    set Gwait_for_it true; \
                } \
                -text OK;
        button $base.cancelButton \
                -command { \
                    global Gwait_for_it; \
                    set Gwait_for_it false; \
                } \
                -text Cancel;

        ###################
        # SETTING GEOMETRY
        ###################
        place $base.nameLabel \
                -x 15 \
                -y 20 \
                -width 39 \
                -height 20 \
                -anchor nw \
                -bordermode ignore;
        place $base.nameEntry \
                -x 60 \
                -y 20 \
                -width 186 \
                -height 22 \
                -anchor nw \
                -bordermode ignore;
        place $base.priorityLabel \
                -x 5 \
                -y 60 \
                -width 54 \
                -height 20 \
                -anchor nw \
                -bordermode ignore;
        place $base.priorityScale \
                -x 55 \
                -y 40 \
                -width 193 \
                -height 47 \
                -anchor nw \
                -bordermode ignore;
        place $base.timeLabel \
                -x 20 \
                -y 110 \
                -anchor nw \
                -bordermode ignore;
        place $base.timeScale \
                -x 55 \
                -y 90 \
                -width 193 \
                -height 47 \
                -anchor nw \
                -bordermode ignore;
        place $base.okButton \
                -x 80 \
                -y 150 \
                -width 58 \
                -height 31 \
                -anchor nw \
                -bordermode ignore;
        place $base.cancelButton \
                -x 150 \
                -y 150 \
                -anchor nw \
                -bordermode ignore;

        return -code ok $base;
    }
}
