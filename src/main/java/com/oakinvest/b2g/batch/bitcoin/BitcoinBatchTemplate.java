package com.oakinvest.b2g.batch.bitcoin;

import com.oakinvest.b2g.domain.bitcoin.BitcoinBlock;
import com.oakinvest.b2g.repository.bitcoin.BitcoinAddressRepository;
import com.oakinvest.b2g.repository.bitcoin.BitcoinBlockRepository;
import com.oakinvest.b2g.repository.bitcoin.BitcoinRepositories;
import com.oakinvest.b2g.repository.bitcoin.BitcoinTransactionOutputRepository;
import com.oakinvest.b2g.service.StatusService;
import com.oakinvest.b2g.service.bitcoin.BitcoinDataService;
import com.oakinvest.b2g.util.bitcoin.mapper.BitcoindToDomainMapper;
import org.mapstruct.factory.Mappers;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * Bitcoin import batch - abstract model.
 * Created by straumat on 27/02/17.
 */
public abstract class BitcoinBatchTemplate {

    /**
     * How many milli seconds in one second.
     */
    private static final float MILLISECONDS_IN_SECONDS = 1000F;

    /**
     * Log separator.
     */
    private static final String LOG_SEPARATOR = "===================================";

    /**
     * Pause to make when there is no block to process (1 second).
     */
    private static final int PAUSE_WHEN_NO_BLOCK_TO_PROCESS = 1000;

    /**
     * Mapper.
     */
    private final BitcoindToDomainMapper mapper = Mappers.getMapper(BitcoindToDomainMapper.class);

    /**
     * Bitcoin block repository.
     */
    private final BitcoinBlockRepository blockRepository;

    /**
     * Bitcoin address repository.
     */
    private final BitcoinAddressRepository addressRepository;

    /**
     * Bitcoin transaction output repository.
     */
    private final BitcoinTransactionOutputRepository transactionOutputRepository;

    /**
     * Bitcoin data service.
     */
    private final BitcoinDataService bitcoinDataService;

    /**
     * Status service.
     */
    private final StatusService status;

    /**
     * Session factory.
     */
    @Autowired
    private SessionFactory sessionFactory;

    /**
     * Neo4j session.
     */
    private Session session;

    /**
     * time of the start of the batch.
     */
    private long batchStartTime;

    /**
     * Constructor.
     *
     * @param newBitcoinRepositories bitcoin repositories
     * @param newBitcoinDataService  bitcoin data service
     * @param newStatus              status
     */
    public BitcoinBatchTemplate(final BitcoinRepositories newBitcoinRepositories, final BitcoinDataService newBitcoinDataService, final StatusService newStatus) {
        this.addressRepository = newBitcoinRepositories.getBitcoinAddressRepository();
        this.blockRepository = newBitcoinRepositories.getBitcoinBlockRepository();
        this.transactionOutputRepository = newBitcoinRepositories.getBitcoinTransactionOutputRepository();
        this.bitcoinDataService = newBitcoinDataService;
        this.status = newStatus;
    }

    /**
     * Initialize sessions.
     */
    @PostConstruct
    public final void loadSession() {
        session = sessionFactory.openSession();
    }

    /**
     * Returns the elapsed time of the batch.
     *
     * @return elapsed time of the batch.
     */
    private float getElapsedTime() {
        return (System.currentTimeMillis() - batchStartTime) / MILLISECONDS_IN_SECONDS;
    }

    /**
     * Execute the batch.
     */
    @Scheduled(fixedDelay = 1)
    @SuppressWarnings("checkstyle:designforextension")
    public void execute() {
        batchStartTime = System.currentTimeMillis();
        addLog(LOG_SEPARATOR);
        try {
            // We retrieve the block to process.
            Optional<Integer> blockHeightToProcess = getBlockHeightToProcess();

            // If there is a block to process.
            if (blockHeightToProcess.isPresent()) {
                // Process the block.
                addLog("Starting to process block " + getFormattedBlockHeight(blockHeightToProcess.get()));
                Optional<BitcoinBlock> blockToProcess = processBlock(blockHeightToProcess.get());

                // If the process ended well.
                blockToProcess.ifPresent((BitcoinBlock bitcoinBlock) -> {
                    // If the block has been well processed, we change the state and we save it.
                    addLog("Saving block data");
                    getBlockRepository().save(bitcoinBlock);
                    addLog("Block " + bitcoinBlock.getFormattedHeight() + " processed in " + getElapsedTime() + " secs");
                    getStatus().setImportedBlockCount(bitcoinBlock.getHeight());
                });
            } else {
                // If there is nothing to process.
                addLog("No block to process");
                Thread.sleep(PAUSE_WHEN_NO_BLOCK_TO_PROCESS);
            }
        } catch (Exception e) {
            addError("An error occurred while processing block : " + e.getMessage(), e);
        } finally {
            getSession().clear();
        }
    }

    /**
     * Getter session.
     *
     * @return session
     */
    private Session getSession() {
        return session;
    }

    /**
     * Return the block to process.
     *
     * @return block to process.
     */
    protected abstract Optional<Integer> getBlockHeightToProcess();

    /**
     * Treat block.
     *
     * @param blockHeight block height to process.
     * @return the block processed
     */
    protected abstract Optional<BitcoinBlock> processBlock(int blockHeight);

    /**
     * Returns the block height in a formatted way.
     *
     * @param blockHeight block height
     * @return formatted block height
     */
    final String getFormattedBlockHeight(final int blockHeight) {
        return String.format("%09d", blockHeight);
    }

    /**
     * Add a logger to the status and the logs.
     *
     * @param message message
     */
    final void addLog(final String message) {
        status.addLog(message);
    }

    /**
     * Add an error to the status and the logs.
     *
     * @param message message
     */
    final void addError(final String message) {
        status.addError(message, null);
    }

    /**
     * Add an error to the status and the logs.
     *
     * @param message message
     * @param e       exception raised.
     */
    private void addError(final String message, final Exception e) {
        status.addError(message, e);
    }

    /**
     * Getter mapper.
     *
     * @return mapper
     */
    final BitcoindToDomainMapper getMapper() {
        return mapper;
    }

    /**
     * Getter blockRepository.
     *
     * @return blockRepository
     */
    final BitcoinBlockRepository getBlockRepository() {
        return blockRepository;
    }

    /**
     * Getter addressRepository.
     *
     * @return addressRepository
     */
    final BitcoinAddressRepository getAddressRepository() {
        return addressRepository;
    }

    /**
     * Getter.
     *
     * @return transactionOutputRepository
     */
    final BitcoinTransactionOutputRepository getTransactionOutputRepository() {
        return transactionOutputRepository;
    }

    /**
     * Getter status.
     *
     * @return status
     */
    final StatusService getStatus() {
        return status;
    }

    /**
     * Getter bitcoin data service.
     *
     * @return bitcoin data service
     */
    final BitcoinDataService getBitcoinDataService() {
        return bitcoinDataService;
    }

}