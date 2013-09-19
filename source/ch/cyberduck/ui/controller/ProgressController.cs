﻿// 
// Copyright (c) 2010-2013 Yves Langisch. All rights reserved.
// http://cyberduck.ch/
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
// 
// Bug fixes, suggestions and comments should be sent to:
// yves@cyberduck.ch
// 

using System;
using System.Collections.Generic;
using System.Globalization;
using System.Text;
using Ch.Cyberduck.Ui.Controller.Threading;
using Ch.Cyberduck.Ui.Winforms;
using Ch.Cyberduck.Ui.Winforms.Controls;
using StructureMap;
using ch.cyberduck.core;
using ch.cyberduck.core.formatter;
using ch.cyberduck.core.transfer;
using ch.cyberduck.core.transfer.copy;
using ch.cyberduck.core.transfer.download;
using ch.cyberduck.core.transfer.synchronisation;
using ch.cyberduck.core.transfer.upload;
using java.util;
using TransferStatus = Ch.Cyberduck.Ui.Winforms.Controls.TransferStatus;

namespace Ch.Cyberduck.Ui.Controller
{
    public class ProgressController : WindowController<IProgressView>, TransferListener, ProgressListener
    {
        /**
         * Formatter for file size
        */
        private readonly SizeFormatter _sizeFormatter = SizeFormatterFactory.get();
        private readonly Transfer _transfer;

        /// <summary>
        /// The current connection status message
        /// </summary>
        private String _messageText;

        public ProgressController(Transfer transfer)
        {
            _transfer = transfer;
            View = ObjectFactory.GetInstance<IProgressView>();
            Init();
        }

        public void message(string message)
        {
            _messageText = message;
            StringBuilder b = new StringBuilder();
            if (null == _messageText)
            {
                // Do not display any progress text when transfer is stopped
                Date timestamp = _transfer.getTimestamp();
                if (null != timestamp)
                {
                    _messageText =
                        UserDateFormatterFactory.get()
                                                .getLongFormat(
                                                    UserDefaultsDateFormatter.ConvertJavaMillisecondsToDotNetMillis(
                                                        timestamp.getTime()), false);
                }
            }
            if (null != _messageText)
            {
                b.Append(_messageText);
            }
            AsyncDelegate d = () => View.MessageText = b.ToString();
            invoke(new SimpleDefaultMainAction(this, d));
        }

        public void start(Transfer t)
        {
            AsyncDelegate d = delegate
                {
                    View.TransferStatus = TransferStatus.InProgress;
                    View.ProgressIndeterminate = true;
                    Progress(String.Empty);
                    View.StatusText = String.Empty;
                };
            invoke(new SimpleDefaultMainAction(this, d));
        }

        public void stop(Transfer t)
        {
            AsyncDelegate d = delegate
                {
                    View.ProgressIndeterminate = true;
                    message(String.Empty);
                    Progress(String.Format(LocaleFactory.localizedString("{0} of {1}"),
                                           _sizeFormatter.format(_transfer.getTransferred()),
                                           _sizeFormatter.format(_transfer.getSize())));
                    View.StatusText =
                        LocaleFactory.localizedString(LocaleFactory.localizedString(_transfer.isComplete()
                                                                                        ? String.Format("{0} complete",
                                                                                                        CultureInfo
                                                                                                            .CurrentCulture
                                                                                                            .TextInfo
                                                                                                            .ToTitleCase
                                                                                                            (
                                                                                                                _transfer
                                                                                                                    .getType
                                                                                                                    ()
                                                                                                                    .name
                                                                                                                    ()))
                                                                                        : "Transfer incomplete",
                                                                                    "Status"), "Status");
                    View.TransferStatus = t.isComplete() ? TransferStatus.Complete : TransferStatus.Incomplete;
                };
            invoke(new SimpleDefaultMainAction(this, d));
        }

        public void progress(TransferProgress tp)
        {
            Progress(tp.getProgress());
            AsyncDelegate d = delegate
                {
                    double transferred = _transfer.getTransferred();
                    double size = _transfer.getSize();
                    if (transferred > 0 && size > 0)
                    {
                        View.ProgressIndeterminate = false;
                        // normalize double to int if size is too big
                        if (size > int.MaxValue)
                        {
                            View.ProgressMaximum = int.MaxValue;
                            View.ProgressValue =
                                Convert.ToInt32(int.MaxValue*transferred/size);
                        }
                        else
                        {
                            View.ProgressMaximum = Convert.ToInt32(size);
                            View.ProgressValue = Convert.ToInt32(transferred);
                        }
                    }
                    else
                    {
                        View.ProgressIndeterminate = true;
                    }
                };
            invoke(new SimpleDefaultMainAction(this, d));
            //UpdateOverallProgress();
        }

/*         TODO wie zu implementieren?
        private void UpdateOverallProgress()
        {
            if (Utils.IsVistaOrLater)
            {
                if (TransferCollection.defaultCollection().numberOfRunningTransfers() +
                    TransferCollection.defaultCollection().numberOfQueuedTransfers() == 0)
                {
                    TransferController.Instance.View.UpdateOverallProgressState(0, 0);
                }
                else
                {
                    double progress = TransferCollection.defaultCollection().getDataTransferred();
                    double maximum = TransferCollection.defaultCollection().getDataSize();
                    TransferController.Instance.View.UpdateOverallProgressState(progress, maximum);
                }
            }
        }
   */

        private void Init()
        {
            Progress(String.Format(LocaleFactory.localizedString("{0} of {1}"),
                                   _sizeFormatter.format(_transfer.getTransferred()),
                                   _sizeFormatter.format(_transfer.getSize())));

            SetIcon();
            SetMessageText();
            SetRootPaths();
            SetTransferStatus();
            View.StatusText = LocaleFactory.localizedString(LocaleFactory.localizedString(_transfer.isComplete()
                                                                                              ? String.Format(
                                                                                                  "{0} complete",
                                                                                                  CultureInfo
                                                                                                      .CurrentCulture
                                                                                                      .TextInfo
                                                                                                      .ToTitleCase(
                                                                                                          _transfer
                                                                                                              .getType()
                                                                                                              .name()))
                                                                                              : "Transfer incomplete",
                                                                                          "Status"), "Status");
        }

        private void Progress(String message)
        {
            invoke(new SimpleDefaultMainAction(this, () => View.ProgressText = message));
        }

        private void SetRootPaths()
        {
            List srcRoots = _transfer.getRoots();
            IList<string> roots = new List<string>();
            for (int i = 0; i < srcRoots.size(); i++)
            {
                Path p = (Path) srcRoots.get(i);
                roots.Add(p.getName());
            }
            View.PopulateRoots(roots);
        }

        private void SetTransferStatus()
        {
            if (_transfer.isRunning())
            {
                View.TransferStatus = TransferStatus.InProgress;
            }
            else
            {
                View.TransferStatus = _transfer.isComplete() ? TransferStatus.Complete : TransferStatus.Incomplete;
            }
        }

        private void SetMessageText()
        {
            message(null);
        }

        private void SetIcon()
        {
            if (_transfer is DownloadTransfer)
            {
                View.TransferDirection = TransferDirection.Download;
            }
            else if (_transfer is UploadTransfer)
            {
                View.TransferDirection = TransferDirection.Upload;
            }
            else if (_transfer is CopyTransfer)
            {
                View.TransferDirection = TransferDirection.Upload;
            }
            else if (_transfer is SyncTransfer)
            {
                View.TransferDirection = TransferDirection.Sync;
            }
        }
    }
}