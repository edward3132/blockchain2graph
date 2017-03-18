package com.oakinvest.b2g.batch.bitcoin;

import com.oakinvest.b2g.domain.bitcoin.BitcoinBlock;
import com.oakinvest.b2g.dto.ext.bitcoin.bitcoind.getblock.GetBlockResponse;
import com.oakinvest.b2g.dto.ext.bitcoin.bitcoind.getblockcount.GetBlockCountResponse;
import com.oakinvest.b2g.dto.ext.bitcoin.bitcoind.getblockhash.GetBlockHashResponse;
import com.oakinvest.b2g.util.bitcoin.BitcoinBatchTemplate;
import org.springframework.stereotype.Component;

/**
 * Bitcoin import blocks batch.
 * Created by straumat on 27/02/17.
 */
@Component
public class BitcoinBatchBlocks extends BitcoinBatchTemplate {

	/**
	 * Log prefix.
	 */
	private static final String PREFIX = "Blocks batch";

	/**
	 * Returns the log prefix to display in each log.
	 */
	@Override
	public final String getLogPrefix() {
		return PREFIX;
	}

	/**
	 * Import data.
	 */
	@Override
	//@Scheduled(initialDelay = BLOCK_IMPORT_INITIAL_DELAY, fixedDelay = PAUSE_BETWEEN_IMPORTS)
	@SuppressWarnings({ "checkstyle:designforextension", "checkstyle:emptyforiteratorpad" })
	public void importData() {
		final long start = System.currentTimeMillis();
		// Block to import.
		final long blockToTreat = getBbr().count() + 1;

		// We retrieve the total number of blocks in bitcoind.
		GetBlockCountResponse blockCountResponse = getBds().getBlockCount();
		if (blockCountResponse.getError() == null) {
			// ---------------------------------------------------------------------------------------------------------
			// If there are still blocks to import...
			final long totalBlockCount = getBds().getBlockCount().getResult();
			if (blockToTreat <= totalBlockCount) {
				// -----------------------------------------------------------------------------------------------------
				// We retrieve the block hash...
				GetBlockHashResponse blockHashResponse = getBds().getBlockHash(blockToTreat);
				if (blockHashResponse.getError() == null) {
					// -------------------------------------------------------------------------------------------------
					// Then we retrieve the block data...
					String blockHash = blockHashResponse.getResult();
					addLog(LOG_SEPARATOR);
					addLog("Starting to import block n°" + getFormattedBlock(blockToTreat) + " (" + blockHash + ")");
					GetBlockResponse blockResponse = getBds().getBlock(blockHash);
					if (blockResponse.getError() == null) {
						// ---------------------------------------------------------------------------------------------
						// Then, if the block doesn't exists, we save it.
						BitcoinBlock block = getBbr().findByHash(blockHash);
						if (block == null) {
							block = getMapper().blockResultToBitcoinBlock(blockResponse.getResult());
							getBbr().save(block);
							addLog("Block n°" + getFormattedBlock(blockToTreat) + " saved with id " + block.getId());
						} else {
							addLog("Block n°" + getFormattedBlock(blockToTreat) + " already saved with id " + block.getId());
						}
						final float elapsedTime = (System.currentTimeMillis() - start) / MILLISECONDS_IN_SECONDS;
						addLog("Block n°" + getFormattedBlock(blockToTreat) + " treated in " + elapsedTime + " secs");
						getLogger().info(getLogPrefix() + " - Block n°" + blockToTreat + " treated in " + elapsedTime + " secs");
					} else {
						// Error while retrieving the block informations.
						addError("Error getting block n°" + getFormattedBlock(blockToTreat) + " informations : " + blockResponse.getError());
					}
				} else {
					// Error while retrieving the block hash.
					addError("Error getting the hash of block n°" + getFormattedBlock(blockToTreat) + " : " + blockHashResponse.getError());
				}
			}
		} else {
			// Error while retrieving the number of blocks in bitcoind.
			addError("Error getting the number of blocks : " + blockCountResponse.getError());
		}
	}

}