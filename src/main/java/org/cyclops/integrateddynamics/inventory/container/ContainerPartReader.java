package org.cyclops.integrateddynamics.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.cyclops.cyclopscore.inventory.IGuiContainerProvider;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.inventory.slot.SlotRemoveOnly;
import org.cyclops.integrateddynamics.core.inventory.container.ContainerMultipart;
import org.cyclops.integrateddynamics.core.part.IPartContainer;
import org.cyclops.integrateddynamics.core.part.PartTarget;
import org.cyclops.integrateddynamics.core.part.aspect.IAspectRead;
import org.cyclops.integrateddynamics.core.part.read.IPartStateReader;
import org.cyclops.integrateddynamics.core.part.read.IPartTypeReader;

/**
 * Container for reader parts.
 * @author rubensworks
 */
public class ContainerPartReader<P extends IPartTypeReader<P, S> & IGuiContainerProvider, S extends IPartStateReader<P>>
        extends ContainerMultipart<P, S, IAspectRead> {

    private final IInventory outputSlots;

    /**
     * Make a new instance.
     * @param partTarget    The target.
     * @param player        The player.
     * @param partContainer The part container.
     * @param partType      The part type.
     * @param partState     The part state.
     */
    public ContainerPartReader(EntityPlayer player, PartTarget partTarget, IPartContainer partContainer, P partType, S partState) {
        super(player, partTarget, partContainer, partType, partState, partType.getReadAspects());

        this.outputSlots = new SimpleInventory(getUnfilteredItemCount(), "temporaryOutputSlots", 1);
        for(int i = 0; i < getUnfilteredItemCount(); i++) {
            addSlotToContainer(new SlotRemoveOnly(outputSlots, i, 144, 27 + ASPECT_BOX_HEIGHT * i));
            disableSlot(i + getUnfilteredItemCount());
        }

        addPlayerInventory(player.inventory, 9, 131);
    }

    protected void disableSlotOutput(int slotIndex) {
        Slot slot = getSlot(slotIndex + getUnfilteredItemCount());
        // Yes I know this is ugly.
        // If you are reading this and know a better way, please tell me.
        slot.xDisplayPosition = Integer.MIN_VALUE;
        slot.yDisplayPosition = Integer.MIN_VALUE;
    }

    protected void enableSlotOutput(int slotIndex, int row) {
        Slot slot = getSlot(slotIndex + getUnfilteredItemCount());
        slot.xDisplayPosition = 144;
        slot.yDisplayPosition = 27 + ASPECT_BOX_HEIGHT * row;
    }

    @Override
    protected void onScroll() {
        super.onScroll();
        for(int i = 0; i < getUnfilteredItemCount(); i++) {
            disableSlotOutput(i);
        }
    }

    @Override
    protected void enableElementAt(int row, int elementIndex, IAspectRead element) {
        super.enableElementAt(row, elementIndex, element);
        enableSlotOutput(elementIndex, row);
    }

    @Override
    protected int getSizeInventory() {
        return getUnfilteredItemCount() * 2; // Input and output slots per item
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!getWorld().isRemote) {
            for (int i = 0; i < getUnfilteredItemCount(); ++i) {
                ItemStack itemstack;
                itemstack = inputSlots.getStackInSlotOnClosing(i);
                if (itemstack != null) {
                    player.dropPlayerItemWithRandomChoice(itemstack, false);
                }
                itemstack = outputSlots.getStackInSlotOnClosing(i);
                if (itemstack != null) {
                    player.dropPlayerItemWithRandomChoice(itemstack, false);
                }
            }
        }
    }

    @Override
    public void onDirty() {
        for(int i = 0; i < getUnfilteredItemCount(); i++) {
            ItemStack itemStack = inputSlots.getStackInSlot(i);
            if(itemStack != null && outputSlots.getStackInSlot(i) == null) {
                ItemStack outputStack = writeAspectInfo(itemStack.copy(), getUnfilteredItems().get(i));
                outputSlots.setInventorySlotContents(i, outputStack);
                inputSlots.decrStackSize(i, 1);
            }
        }
    }

}
