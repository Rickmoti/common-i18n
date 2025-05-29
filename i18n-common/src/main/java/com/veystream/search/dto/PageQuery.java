package com.veystream.search.dto;

public class PageQuery {
   private int page; 
   private int size;

   public PageQuery(int page, int size) { 
       this.page = page; 
       this.size = size; 
   }

   public int getPage() { 
       return page; 
   }
   
   public int getSize() { 
       return size; 
   }

   // Optional: Add setters if needed, or a default constructor

   public int getPageOffset() { 
       return page * size; 
   }
}
