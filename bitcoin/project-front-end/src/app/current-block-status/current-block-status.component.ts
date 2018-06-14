import {Component, HostBinding, OnInit} from '@angular/core';
import {Blockchain2graphService} from '../blockchain2graph.service';
import {CurrentBlockStatus, CurrentBlockStatusProcessStep} from '../project-back-end';

@Component({
  selector: 'app-current-block-status',
  templateUrl: './current-block-status.component.html',
  styleUrls: ['./current-block-status.component.css']
})
export class CurrentBlockStatusComponent implements OnInit {

  // Static values.
  static readonly blockHeightNonAvailable = -1;
  static readonly noBlockToProcessDescription = 'No block to process';
  static readonly newBlockToProcessDescription = 'New block to process';
  static readonly loadingTransactionsFromBitcoinCoreDescription = 'Loading transactions from bitcoin core...';
  static readonly processingAddressesDescription = 'Processing addresses...';
  static readonly processingTransactionsDescription = 'Processing transactions...';
  static readonly savingBlockDescription = 'Saving block...';
  static readonly savedBlockDescription = 'Block saved';

  // TODO Ask Cyrille.
  @HostBinding('class') class = 'card mb-4 text-white';

  // Component details.
  blockHeight = CurrentBlockStatusComponent.noBlockToProcessDescription;
  viewDetails = false;
  processStepDescription = CurrentBlockStatusComponent.noBlockToProcessDescription;
  progression = 0;

  /**
   * Constructor.
   * @param {Blockchain2graphService} blockchain2graphService blockchain2graph service.
   */
  constructor(private blockchain2graphService: Blockchain2graphService) { }

  /**
   * Subscribe to block change status.
   */
  ngOnInit() {
    // We subscribe to the change of block status.
    this.blockchain2graphService.currentBlockStatus.subscribe((blockStatus: CurrentBlockStatus) => {
      this.processStatus(blockStatus);
    });
  }

  /**
   * Process the new block status.
   * @param {CurrentBlockStatus} blockStatus current block status.
   */
  private processStatus(blockStatus: CurrentBlockStatus) {
    this.setBlockHeight(blockStatus.blockHeight);
    switch (blockStatus.processStep) {

      // Nothing to process.
      case CurrentBlockStatusProcessStep.NO_BLOCK_TO_PROCESS:
        this.processStepDescription = CurrentBlockStatusComponent.noBlockToProcessDescription;
        this.viewDetails = false;
        break;

      // New block to process.
      case CurrentBlockStatusProcessStep.NEW_BLOCK_TO_PROCESS:
        this.processStepDescription = CurrentBlockStatusComponent.newBlockToProcessDescription;
        this.viewDetails = true;
        break;

      // Loading transactions from bitcoin core.
      case CurrentBlockStatusProcessStep.LOADING_TRANSACTIONS_FROM_BITCOIN_CORE:
        this.processStepDescription = CurrentBlockStatusComponent.loadingTransactionsFromBitcoinCoreDescription;
        this.viewDetails = true;
        this.setProgression(blockStatus.loadedTransactions, blockStatus.transactionCount);
        break;

      // Processing addresses.
      case CurrentBlockStatusProcessStep.PROCESSING_ADDRESSES:
        this.processStepDescription = CurrentBlockStatusComponent.processingAddressesDescription;
        this.viewDetails = true;
        this.setProgression(blockStatus.processedAddresses, blockStatus.addressCount);
        break;

      // Processing transactions.
      case CurrentBlockStatusProcessStep.PROCESSING_TRANSACTIONS:
        this.processStepDescription = CurrentBlockStatusComponent.processingTransactionsDescription;
        this.viewDetails = true;
        this.setProgression(blockStatus.processedTransactions, blockStatus.transactionCount);
        break;

      // Saving block.
      case CurrentBlockStatusProcessStep.SAVING_BLOCK:
        this.processStepDescription = CurrentBlockStatusComponent.savingBlockDescription;
        this.viewDetails = true;
        this.setProgression(100, 100);
        break;

      // Block saved.
      case CurrentBlockStatusProcessStep.BLOCK_SAVED:
        this.processStepDescription = CurrentBlockStatusComponent.savedBlockDescription;
        this.viewDetails = true;
        this.setProgression(100, 100);
        break;
    }
  }

  /**
   * Sets block height as a string and with 8 characters (leading 0).
   * @param {number} height block height
   */
  public setBlockHeight(height: number) {
    if (height === CurrentBlockStatusComponent.blockHeightNonAvailable) {
      // Nothing.
      this.blockHeight = CurrentBlockStatusComponent.noBlockToProcessDescription;
    } else {
      // Format block height.
      this.blockHeight = 'Block ' + height.toString().padStart(8, '0');
    }
  }

  /**
   * Set the progression bar.
   * @param {number} current current value
   * @param {number} total total value
   */
  public setProgression(current: number, total: number) {
    if (total !== 0) {
      this.progression = Math.trunc((current / total) * 100);
    } else {
      this.progression = 0;
    }
  }

}
